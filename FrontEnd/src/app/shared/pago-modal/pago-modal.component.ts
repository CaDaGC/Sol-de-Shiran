import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, Input, PLATFORM_ID, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ReservaResponse } from '../../models/reserva.model';
import { AuthService } from '../../services/auth';
import { PagosService } from '../../services/pagos.service';

@Component({
  selector: 'app-pago-modal', standalone: true, imports: [CommonModule],
  templateUrl: './pago-modal.component.html', styleUrl: './pago-modal.component.css'
})
export class PagoModalComponent {
  @Input({ required: true }) reserva!: ReservaResponse;
  @Input() nombreCabana = 'Cabaña';
  @Input() textoBoton = '';
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
    if (new Date(this.reserva.expiraEn).getTime() <= Date.now()) {
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
      window.location.assign(url.href);
    } catch (error: any) {
      this.mensaje.set(error?.error?.detail ?? error?.error?.message ?? error?.message ?? 'No se pudo abrir Mercado Pago.');
      this.enviandoPago.set(false);
    }
  }
}
