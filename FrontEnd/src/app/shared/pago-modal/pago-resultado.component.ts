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
  template: `<main class="return-page"><section class="return-card">
    <app-pago-estado [estado]="estado()" [mensaje]="mensaje()" [monto]="pago()?.monto ?? null"
      [idReserva]="pago()?.idReserva ?? 0" [transaccion]="pago()?.idTransaccion ?? 0"></app-pago-estado>
    <button type="button" [disabled]="cargando()" (click)="consultar()">{{ cargando() ? 'Consultando…' : 'Actualizar estado' }}</button>
    <a routerLink="/catalogo">Volver al catálogo</a>
  </section></main>`,
  styles: [`.return-page{min-height:100dvh;display:grid;place-items:center;padding:24px;box-sizing:border-box;background:#edf2eb}.return-card{width:min(600px,100%);padding:24px;box-sizing:border-box;background:white;border-radius:18px;box-shadow:0 20px 60px #203d2e15}.return-card button,.return-card a{display:block;text-align:center;font:600 .9rem Arial,sans-serif;margin:14px auto;padding:12px 18px;border-radius:8px}.return-card button{border:0;background:#315b3d;color:white;cursor:pointer}.return-card a{color:#315b3d}`]
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
