import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, PLATFORM_ID, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CheckoutRetornoService } from '../../services/checkout-retorno.service';
import { PagoEstadoComponent } from './pago-estado.component';
import { ReservaResponse, CotizacionReservaResponse } from '../../models/reserva.model';
import { AuthService } from '../../services/auth';
import { PagosService } from '../../services/pagos.service';

@Component({
  selector: 'app-pago-modal', standalone: true, imports: [CommonModule, PagoEstadoComponent],
  templateUrl: './pago-modal.component.html', styleUrl: './pago-modal.component.css'
})
export class PagoModalComponent implements OnInit, OnDestroy {
  @Input({ required: true }) reserva!: ReservaResponse;
  @Input() nombreCabana = 'Cabaña';
  @Input() transaccionRetorno = 0;
  @Input() contexto: { adultos: number; ninos: number; imagen: number; cotizacion?: CotizacionReservaResponse | null } = { adultos: 1, ninos: 0, imagen: 0 };
  @Output() confirmado = new EventEmitter<void>();
  readonly estadoPago = signal('');
  readonly montoVerificado = signal<number | null>(null);
  private readonly retorno = inject(CheckoutRetornoService);
  private reintento?: ReturnType<typeof setTimeout>;
  private destruido = false;
  private intentos = 0;
  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId) && this.transaccionRetorno > 0) {
      this.abierto.set(true); void this.verificarRetorno();
    }
  }
  ngOnDestroy(): void { this.destruido = true; clearTimeout(this.reintento); }
  async verificarRetorno(): Promise<void> {
    if (this.enviandoPago() || this.destruido) return;
    clearTimeout(this.reintento); this.enviandoPago.set(true); this.estadoPago.set('PENDIENTE'); this.mensaje.set('');
    try {
      const pago = await firstValueFrom(this.pagosService.consultarEstado(this.transaccionRetorno));
      if (this.destruido) return;
      if (pago.idReserva !== this.reserva.idReserva) throw new Error('La operación no corresponde a esta reserva.');
      this.montoVerificado.set(pago.monto);
      this.estadoPago.set(pago.estado);
      if (pago.estado === 'APROBADA') this.confirmado.emit();
      else if (pago.estado === 'PENDIENTE' && ++this.intentos < 6)
        this.reintento = setTimeout(() => void this.verificarRetorno(), 5000);
    } catch (error: any) {
      if (!this.destruido) { this.estadoPago.set('ERROR'); this.mensaje.set(error?.error?.detail ?? error?.message ?? 'Vuelve a consultar en unos momentos.'); }
    } finally { if (!this.destruido) this.enviandoPago.set(false); }
  }

  readonly abierto = signal(false);
  readonly enviandoPago = signal(false);
  readonly mensaje = signal('');
  private readonly platformId = inject(PLATFORM_ID);
  constructor(private readonly pagosService: PagosService,
    private readonly authService: AuthService, private readonly router: Router) {}

  abrir(): void {
    if (!this.authService.estaAutenticado()) {
      void this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    if (!this.estadoPago() && new Date(this.reserva.expiraEn).getTime() <= Date.now()) {
      this.mensaje.set('La reserva temporal venció. Vuelve a consultar las fechas.');
      return;
    }
    this.mensaje.set('');
    this.abierto.set(true);
  }
  cerrar(): void { if (!this.enviandoPago()) this.abierto.set(false); }
  formatearMonto(monto: number): string {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(monto);
  }
  async pagar(): Promise<void> {
    if (this.enviandoPago() || !isPlatformBrowser(this.platformId)) return;
    const email = this.authService.usuarioActual()?.email;
    if (!email) { this.mensaje.set('Inicia sesión nuevamente para continuar.'); return; }
    this.enviandoPago.set(true);
    this.mensaje.set('');
    try {
      const orden = await firstValueFrom(this.pagosService.crearOrden(this.reserva.idReserva, email));
      const url = new URL(orden.checkoutUrl);
      if (url.protocol !== 'https:' || !/(^|\.)mercadopago\.(com|com\.pe)$/.test(url.hostname)) {
        throw new Error('Mercado Pago devolvió una dirección de checkout inesperada.');
      }
      this.retorno.guardar({ transaccion: orden.idTransaccion, reserva: this.reserva,
        nombreCabana: this.nombreCabana, url: this.router.url, ...this.contexto,
        scroll: window.scrollY, fecha: Date.now() });
      window.location.assign(url.href);
    } catch (error: any) {
      this.mensaje.set(error?.error?.detail ?? error?.error?.message ?? error?.message ?? 'No se pudo abrir Mercado Pago.');
      this.enviandoPago.set(false);
    }
  }
}
