import { Component, DestroyRef, OnDestroy, inject, input, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { firstValueFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PedidosRestauranteService } from '../../services/pedidos-restaurante.service';
import { mensajeErrorApi } from '../../core/error-api';
import { VentanaModalComponent } from '../ventana-modal/ventana-modal.component';

interface DestinoPdf { createWritable(): Promise<{ write(archivo: Blob): Promise<void>; close(): Promise<void> }>; }
type VentanaPdf = Window & { showSaveFilePicker?: (opciones: unknown) => Promise<DestinoPdf> };

@Component({ selector: 'app-comanda', standalone: true, imports: [VentanaModalComponent],
  template: `<button type="button" class="btn btn-outline-success" (click)="abrir()">Ver comanda</button>
  @if (abierta()) {
    <app-ventana-modal [titulo]="'Comanda ' + codigo()" [amplia]="true" [ocupado]="descargando()" (cerrar)="cerrar()">
      <p class="small text-secondary">Documento para cocina y reparto. No acredita el cobro del pedido.</p>
      @if (cargando()) { <p role="status">Cargando comanda…</p> }
      @if (error()) { <div class="alert alert-danger" role="alert">{{ error() }}</div> }
      @if (vista()) { <iframe class="visor-comanda" [src]="vista()" title="Comanda del pedido"></iframe> }
      <div class="d-flex gap-2 flex-wrap mt-3"><button type="button" class="btn btn-success" [disabled]="!vista() || descargando()" (click)="descargar()">{{ descargando() ? 'Guardando…' : 'Descargar comanda' }}</button>
      @if (error() && !vista()) { <button type="button" class="btn btn-outline-success" [disabled]="cargando()" (click)="cargar()">Reintentar</button> }
      <button type="button" class="btn btn-outline-secondary" [disabled]="descargando()" (click)="cerrar()">Cerrar</button></div>
    </app-ventana-modal>
  }`, styles: [`.visor-comanda{width:100%;height:58dvh;min-height:300px;border:1px solid var(--color-line);border-radius:10px;background:#fff}`],
})
export class ComandaComponent implements OnDestroy {
  readonly idRestaurante = input.required<number>();
  readonly idPedido = input.required<number>();
  readonly codigo = input.required<string>();
  readonly abierta = signal(false);
  readonly cargando = signal(false);
  readonly descargando = signal(false);
  readonly error = signal('');
  readonly vista = signal<SafeResourceUrl | null>(null);
  private readonly api = inject(PedidosRestauranteService);
  private readonly sanitizador = inject(DomSanitizer);
  private readonly destruir = inject(DestroyRef);
  private archivo: Blob | null = null;
  private url: string | null = null;
  private solicitud = 0;
  abrir(): void { this.abierta.set(true); void this.cargar(); }
  async cargar(): Promise<void> {
    const solicitud = ++this.solicitud; this.cargando.set(true); this.error.set(''); this.liberar();
    try {
      const archivo = await firstValueFrom(this.api.comanda(this.idRestaurante(), this.idPedido()).pipe(takeUntilDestroyed(this.destruir)));
      if (solicitud !== this.solicitud) return;
      this.archivo = archivo; this.url = URL.createObjectURL(archivo);
      this.vista.set(this.sanitizador.bypassSecurityTrustResourceUrl(this.url));
    } catch (error) {
      if (solicitud !== this.solicitud) return;
      if (error instanceof HttpErrorResponse && error.error instanceof Blob) {
        try { const datos = JSON.parse(await error.error.text()); if (solicitud === this.solicitud) this.error.set(datos.detail || 'No se pudo obtener la comanda.'); }
        catch { this.error.set('No se pudo obtener la comanda. Inténtalo nuevamente.'); }
      } else this.error.set(mensajeErrorApi(error, 'No se pudo obtener la comanda.'));
    } finally { if (solicitud === this.solicitud) this.cargando.set(false); }
  }
  async descargar(): Promise<void> {
    if (!this.archivo || !this.url || this.descargando()) return;
    this.descargando.set(true); this.error.set('');
    try {
      const nombre = 'comanda-' + this.codigo() + '.pdf'; const ventana = window as VentanaPdf;
      if (ventana.showSaveFilePicker) {
        const destino = await ventana.showSaveFilePicker({ suggestedName: nombre, types: [{ description: 'Documento PDF', accept: { 'application/pdf': ['.pdf'] } }] });
        const salida = await destino.createWritable(); await salida.write(this.archivo); await salida.close();
      } else {
        const enlace = document.createElement('a'); enlace.href = this.url; enlace.download = nombre; document.body.append(enlace); enlace.click(); enlace.remove();
      }
    } catch (error) { if (!(error instanceof DOMException && error.name === 'AbortError')) this.error.set('No se pudo guardar el documento.'); }
    finally { this.descargando.set(false); }
  }
  cerrar(): void { if (this.descargando()) return; this.solicitud++; this.abierta.set(false); this.cargando.set(false); this.liberar(); }
  private liberar(): void { this.vista.set(null); this.archivo = null; if (this.url) URL.revokeObjectURL(this.url); this.url = null; }
  ngOnDestroy(): void { this.solicitud++; this.liberar(); }
}
