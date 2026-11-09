import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, OnDestroy, PLATFORM_ID, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CheckoutRetornoService } from '../../services/checkout-retorno.service';
import { PagoEstadoComponent } from './pago-estado.component';
import { PagoResponse } from '../../models/pago.model';
import { PagosService } from '../../services/pagos.service';

@Component({
  selector: 'app-pago-resultado', standalone: true, imports: [CommonModule, RouterLink, PagoEstadoComponent],
  template: `<main class="result-page">
    <img src="/images/logo-sol-shiran.png" alt="Complejo Turístico Sol de Shirán" width="1698" height="926"
      class="result-logo">
    <h1>Estado de tu pago</h1>
    <app-pago-estado [estado]="estado()" [mensaje]="mensaje()" [monto]="pago()?.monto ?? null"
      [idReserva]="pago()?.idReserva ?? 0" [transaccion]="pago()?.idTransaccion ?? 0"></app-pago-estado>
    <button type="button" class="primary-button" [disabled]="cargando()" (click)="consultar()">{{ cargando() ? 'Consultando…' : 'Actualizar estado' }}</button>
    <p><a routerLink="/catalogo">Volver al catálogo</a></p>
  </main>`,
  styles: `
    .result-page { width: min(620px, calc(100% - 32px)); margin: 48px auto; padding: 32px; border: 1px solid var(--color-line); border-radius: 18px; background: #fff; text-align: center; }
    .result-logo { display: block; width: min(280px, 100%); height: auto; margin: 0 auto 28px; padding: 20px; border-radius: 12px; background: var(--color-forest); }
    h1 { color: var(--color-forest); font-size: 2rem; }
    p { color: var(--color-muted); line-height: 1.7; }
    a { color: var(--color-green); font-size: .85rem; }
  `
})
export class PagoResultadoComponent implements OnInit, OnDestroy {
  readonly estado = signal('PENDIENTE');
  readonly pago = signal<PagoResponse | null>(null);
  private readonly retorno = inject(CheckoutRetornoService);
  private readonly router = inject(Router);
  readonly mensaje = signal('Consultando el estado de la operación…');
  readonly cargando = signal(false);
  private readonly platformId = inject(PLATFORM_ID);
  private reintento?: ReturnType<typeof setTimeout>;
  private intentos = 0;
  private destruido = false;
  constructor(private readonly route: ActivatedRoute, private readonly pagos: PagosService) {}
  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const id = Number(this.route.snapshot.queryParamMap.get('transaccion'));
    const contexto = this.retorno.obtener(id);
    if (contexto) {
      const url = this.router.parseUrl(contexto.url);
      url.queryParams = { ...url.queryParams, transaccion: id };
      void this.router.navigateByUrl(url, { replaceUrl: true });
      return;
    }
    void this.consultar();
  }
  ngOnDestroy(): void { this.destruido = true; clearTimeout(this.reintento); }
  async consultar(): Promise<void> {
    const id = Number(this.route.snapshot.queryParamMap.get('transaccion'));
    if (!Number.isSafeInteger(id) || id <= 0) { this.estado.set('ERROR'); this.mensaje.set('No se encontró la transacción.'); return; }
    if (this.cargando()) return;
    clearTimeout(this.reintento);
    this.intentos++;
    this.cargando.set(true);
    try {
      // El resultado de la URL nunca se usa como comprobante de pago.
      const pago = await firstValueFrom(this.pagos.consultarEstado(id));
      if (this.destruido) return;
      this.pago.set(pago); this.estado.set(pago.estado);
      this.mensaje.set(pago.estado === 'APROBADA' ? 'Pago aprobado. Tu reserva quedó confirmada.'
        : pago.estado === 'RECHAZADA' ? 'La operación no se completó. Revisa el estado en Mercado Pago.'
        : 'Tu pago sigue pendiente. Puedes actualizar su estado en unos momentos.');
      if (pago.estado === 'PENDIENTE' && this.intentos < 6 && !this.destruido) {
        this.reintento = setTimeout(() => void this.consultar(), 5000);
      }
    } catch (error: any) {
      this.estado.set('ERROR');
      this.mensaje.set(error?.error?.detail ?? 'No se pudo verificar el pago. Inicia sesión y vuelve a consultar.');
    } finally { this.cargando.set(false); }
  }
}
