import { Component, OnDestroy, computed, input, output, signal } from '@angular/core';
import { imagenUrl } from '../../core/imagen-url';

export interface SeleccionImagenMenu { archivo: File | null; quitar: boolean; valida: boolean; }

@Component({ selector: 'app-imagen-menu-formulario', standalone: true,
  template: `<fieldset class="imagen-menu"><legend class="form-label">{{ etiqueta() }}</legend>
    @if (vista()) { <img class="vista-imagen" [src]="vista()" alt="Vista previa de la imagen" /> }
    @else { <div class="sin-imagen">Añade una imagen de presentación.</div> }
    <label class="form-label d-block mt-3">Seleccionar archivo<input class="form-control mt-2" type="file" name="imagenMenu" accept="image/png,image/jpeg" [disabled]="ocupado()" (change)="seleccionar($event)" /></label>
    <p class="small text-secondary mb-2">JPG o PNG de hasta 2 MB. Los cambios de imagen se aplicarán al guardar el formulario.</p>
    @if (error()) { <p class="text-danger small" role="alert">{{ error() }}</p> }
    @if (vista()) { <button type="button" class="btn btn-outline-secondary btn-sm" [disabled]="ocupado()" (click)="quitar()">Quitar imagen</button> }
    @if (quitada()) { <p class="small text-secondary mb-0">La imagen se quitará cuando guardes los cambios.</p> }
  </fieldset>`,
  styles: [`.imagen-menu{border:1px solid var(--color-line);padding:18px;border-radius:14px;background:#fff}.imagen-menu legend{float:none;width:auto;font-size:.85rem;margin-bottom:12px}.vista-imagen{width:100%;height:200px;object-fit:contain;border-radius:10px;background:#edf1e8}.sin-imagen{display:grid;place-items:center;min-height:100px;padding:20px;background:#edf1e8;border-radius:10px;color:var(--color-muted);font-size:.85rem;text-align:center}`],
})
export class ImagenMenuFormularioComponent implements OnDestroy {
  readonly actual = input<string | null>(null);
  readonly etiqueta = input('Imagen');
  readonly ocupado = input(false);
  readonly seleccion = output<SeleccionImagenMenu>();
  readonly temporal = signal('');
  readonly quitada = signal(false);
  readonly error = signal('');
  readonly vista = computed(() => this.temporal() || (!this.quitada() ? imagenUrl(this.actual()) : ''));
  private campo: HTMLInputElement | null = null;
  seleccionar(evento: Event): void {
    this.campo = evento.target as HTMLInputElement;
    const archivo = this.campo.files?.[0];
    if (!archivo) return;
    this.error.set(''); this.campo.setCustomValidity('');
    if (!['image/png', 'image/jpeg'].includes(archivo.type) || archivo.size > 2 * 1024 * 1024) {
      const mensaje = 'Selecciona una imagen JPG o PNG de hasta 2 MB.';
      this.error.set(mensaje); this.campo.setCustomValidity(mensaje);
      this.seleccion.emit({ archivo: null, quitar: this.quitada(), valida: false }); return;
    }
    this.liberar(); this.temporal.set(URL.createObjectURL(archivo)); this.quitada.set(false);
    this.seleccion.emit({ archivo, quitar: false, valida: true });
  }
  quitar(): void {
    this.liberar(); this.error.set(''); this.quitada.set(true);
    if (this.campo) { this.campo.value = ''; this.campo.setCustomValidity(''); }
    this.seleccion.emit({ archivo: null, quitar: true, valida: true });
  }
  private liberar(): void { if (this.temporal()) URL.revokeObjectURL(this.temporal()); this.temporal.set(''); }
  ngOnDestroy(): void { this.liberar(); }
}
