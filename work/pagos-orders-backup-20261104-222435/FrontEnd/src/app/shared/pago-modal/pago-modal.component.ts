import { CommonModule, isPlatformBrowser } from '@angular/common';
import {
  AfterViewInit,
  Component,
  Input,
  OnDestroy,
  PLATFORM_ID,
  inject,
  signal
} from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { MERCADO_PAGO_CONFIG } from '../../core/mercadopago.config';
import { PagoResponse } from '../../models/pago.model';
import { ReservaResponse } from '../../models/reserva.model';
import { AuthService } from '../../services/auth';
import { PagosService } from '../../services/pagos.service';

interface MercadoPagoBrickController {
  unmount: () => void;
}

interface MercadoPagoBrickBuilder {
  create: (
    type: string,
    containerId: string,
    settings: Record<string, unknown>
  ) => Promise<MercadoPagoBrickController>;
}

interface MercadoPagoInstance {
  bricks: () => MercadoPagoBrickBuilder;
}

interface MercadoPagoConstructor {
  new (publicKey: string, options?: { locale?: string }): MercadoPagoInstance;
}

declare global {
  interface Window {
    MercadoPago?: MercadoPagoConstructor;
  }
}

@Component({
  selector: 'app-pago-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pago-modal.component.html',
  styleUrl: './pago-modal.component.css'
})
export class PagoModalComponent implements AfterViewInit, OnDestroy {
  @Input({ required: true }) reserva!: ReservaResponse;
  @Input() nombreCabana = 'Cabaña';

  readonly abierto = signal(false);
  readonly cargandoBrick = signal(false);
  readonly enviandoPago = signal(false);
  readonly mensaje = signal('');
  readonly resultado = signal<PagoResponse | null>(null);

  private readonly platformId = inject(PLATFORM_ID);
  private controladorBrick?: MercadoPagoBrickController;
  private componenteDestruido = false;

  constructor(
    private readonly pagosService: PagosService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngAfterViewInit(): void {
    // El Brick se carga únicamente al abrir este modal.
  }

  ngOnDestroy(): void {
    this.componenteDestruido = true;
    this.desmontarBrick();
  }

  abrir(): void {
    if (!this.authService.estaAutenticado()) {
      void this.router.navigate(['/login'], {
        queryParams: { returnUrl: this.router.url }
      });
      return;
    }

    if (new Date(this.reserva.expiraEn).getTime() <= Date.now()) {
      this.mensaje.set(
        'La reserva temporal venció. Cierra este modal y vuelve a consultar las fechas.'
      );
      return;
    }

    if (!MERCADO_PAGO_CONFIG.publicKey
        || MERCADO_PAGO_CONFIG.publicKey.includes('PEGA_AQUI')) {
      this.mensaje.set(
        'Configura la Public Key de prueba en mercadopago.config.ts.'
      );
      return;
    }

    this.mensaje.set('');
    this.resultado.set(null);
    this.abierto.set(true);
    this.cargandoBrick.set(true);

    // Espera a que Angular pinte el contenedor antes de crear el Brick.
    setTimeout(() => {
      void this.cargarYCrearBrick();
    }, 0);
  }

  cerrar(): void {
    this.desmontarBrick();
    this.abierto.set(false);
    this.cargandoBrick.set(false);
  }

  formatearMonto(monto: number): string {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(monto);
  }

  private async cargarYCrearBrick(): Promise<void> {
    if (!isPlatformBrowser(this.platformId) || this.componenteDestruido) {
      return;
    }

    try {
      await this.cargarSdk();

      if (!this.abierto() || !window.MercadoPago) {
        return;
      }

      const contenedor = document.getElementById('paymentBrick_container');
      if (!contenedor) {
        throw new Error('No se encontró el contenedor del Payment Brick.');
      }

      const mp = new window.MercadoPago(
        MERCADO_PAGO_CONFIG.publicKey,
        { locale: 'es-PE' }
      );

      const bricksBuilder = mp.bricks();

      this.controladorBrick = await bricksBuilder.create(
        'payment',
        'paymentBrick_container',
        {
          initialization: {
            amount: this.reserva.montoAdelanto
          },
          customization: {
            paymentMethods: {
              creditCard: 'all',
              debitCard: 'all',
              prepaidCard: 'all'
            }
          },
          callbacks: {
            onReady: () => {
              this.cargandoBrick.set(false);
            },
            onSubmit: ({ formData }: { formData: any }) =>
              this.enviarPago(formData),
            onError: (error: unknown) => {
              console.error('Error del Payment Brick:', error);
              this.mensaje.set(
                'Mercado Pago informó un error en el formulario. Revisa los datos e inténtalo nuevamente.'
              );
              this.cargandoBrick.set(false);
            }
          }
        }
      );

      this.cargandoBrick.set(false);
    } catch (error) {
      console.error('No se pudo cargar el Payment Brick:', error);
      this.cargandoBrick.set(false);
      this.mensaje.set(
        'No se pudo cargar el formulario de pago. Verifica tu conexión y la Public Key de prueba.'
      );
    }
  }

  private async enviarPago(formData: any): Promise<void> {
    if (this.enviandoPago()) return;

    const emailBrick = formData?.payer?.email;
    const emailSesion = this.authService.usuarioActual()?.email;
    const payerEmail = emailBrick || emailSesion;
    const identificationType = formData?.payer?.identification?.type;
    const identificationNumber = formData?.payer?.identification?.number;

    if (!payerEmail || !identificationType || !identificationNumber) {
      this.mensaje.set(
        'El formulario no entregó el correo o documento del pagador. Completa nuevamente los datos del titular.'
      );
      throw new Error('Faltan datos requeridos del pagador.');
    }

    this.enviandoPago.set(true);
    this.mensaje.set('');

    try {
      const request = {
        token: formData?.token ?? '',
        paymentMethodId:
          formData?.payment_method_id ?? formData?.paymentMethodId ?? '',
        installments: Number(formData?.installments ?? 1),
        issuerId: formData?.issuer_id != null
          ? String(formData.issuer_id)
          : null,
        payerEmail,
        identificationType: String(identificationType),
        identificationNumber: String(identificationNumber)
      };

      const respuesta = await firstValueFrom(
        this.pagosService.procesarPagoBrick(
          this.reserva.idReserva,
          request
        )
      );

      this.resultado.set(respuesta);

      if (respuesta.estado === 'APROBADA') {
        this.mensaje.set(
          'Pago aprobado. Tu reserva quedó confirmada.'
        );
      } else if (respuesta.estado === 'PENDIENTE') {
        this.mensaje.set(
          'El pago quedó pendiente. Mercado Pago actualizará su estado cuando se procese.'
        );
      } else {
        this.mensaje.set(
          'El pago fue rechazado. Revisa los datos o prueba otro medio de pago.'
        );
      }
    } catch (error: any) {
      console.error('No se pudo procesar el pago:', error);
      this.mensaje.set(
        error?.error?.detail ??
        error?.error?.message ??
        'No se pudo enviar el pago. Inténtalo nuevamente.'
      );
      throw error;
    } finally {
      this.enviandoPago.set(false);
    }
  }

  private async cargarSdk(): Promise<void> {
    if (window.MercadoPago) return;

    const existente = document.getElementById(
      'mercado-pago-sdk'
    ) as HTMLScriptElement | null;

    if (existente) {
      await new Promise<void>((resolve, reject) => {
        existente.addEventListener('load', () => resolve(), { once: true });
        existente.addEventListener(
          'error',
          () => reject(new Error('No se pudo cargar Mercado Pago.js')),
          { once: true }
        );

        // Si el script ya terminó de cargar, el constructor estará disponible.
        if (window.MercadoPago) resolve();
      });
      return;
    }

    await new Promise<void>((resolve, reject) => {
      const script = document.createElement('script');
      script.id = 'mercado-pago-sdk';
      script.src = 'https://sdk.mercadopago.com/js/v2';
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () =>
        reject(new Error('No se pudo descargar Mercado Pago.js'));
      document.head.appendChild(script);
    });
  }

  private desmontarBrick(): void {
    try {
      this.controladorBrick?.unmount();
    } catch (error) {
      console.warn('No se pudo desmontar el Payment Brick:', error);
    }

    this.controladorBrick = undefined;

    const contenedor = document.getElementById('paymentBrick_container');
    if (contenedor) {
      contenedor.replaceChildren();
    }
  }
}
