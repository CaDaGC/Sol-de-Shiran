import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { PagosService } from '../../services/pagos.service';

@Component({
  selector: 'app-pago-resultado', standalone: true, imports: [CommonModule, RouterLink],
  template: `<main class="result-page">
    <img src="/images/logo-sol-shiran.png" alt="Complejo Turístico Sol de Shirán" width="1698" height="926"
      class="result-logo">
    <h1>Estado de tu pago</h1><p role="status">{{ mensaje() }}</p>
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
export class PagoResultadoComponent implements OnInit {
  readonly mensaje = signal('Consultando el estado de la operación…');
  readonly cargando = signal(false);
  private readonly platformId = inject(PLATFORM_ID);
  constructor(private readonly route: ActivatedRoute, private readonly pagos: PagosService) {}
  ngOnInit(): void { if (isPlatformBrowser(this.platformId)) void this.consultar(); }
  async consultar(): Promise<void> {
    const id = Number(this.route.snapshot.queryParamMap.get('transaccion'));
    if (!Number.isSafeInteger(id) || id <= 0) { this.mensaje.set('No se encontró la transacción.'); return; }
    if (this.cargando()) return;
    this.cargando.set(true);
    try {
      // El resultado de la URL nunca se usa como comprobante de pago.
      const pago = await firstValueFrom(this.pagos.consultarEstado(id));
      this.mensaje.set(pago.estado === 'APROBADA' ? 'Pago aprobado. Tu reserva quedó confirmada.'
        : pago.estado === 'RECHAZADA' ? 'La operación no se completó. Revisa el estado en Mercado Pago.'
        : 'Tu pago sigue pendiente. Puedes actualizar su estado en unos momentos.');
    } catch (error: any) {
      this.mensaje.set(error?.error?.detail ?? 'No se pudo verificar el pago. Inicia sesión y vuelve a consultar.');
    } finally { this.cargando.set(false); }
  }
}
