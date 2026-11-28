import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnChanges, OnInit, SimpleChanges, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, forkJoin, map, of, switchMap, tap } from 'rxjs';
import { CategoriaMenu, ProductoMenu, ProductoMenuDatos } from '../../../models/restaurante.model';
import { RestauranteService } from '../../../services/restaurante.service';
import { NotificacionesService } from '../../../services/notificaciones.service';
import { mensajeErrorApi } from '../../../core/error-api';
import { imagenUrl } from '../../../core/imagen-url';
import { ConfirmarDirective } from '../../../shared/confirmar/confirmar.directive';
import { FormularioDirective } from '../../../shared/formulario/formulario.directive';
import { VentanaModalComponent } from '../../../shared/ventana-modal/ventana-modal.component';
import { ImagenMenuFormularioComponent, SeleccionImagenMenu } from '../../../shared/imagen-menu-formulario/imagen-menu-formulario.component';
import { IconoComponent } from '../../../shared/icono/icono.component';

@Component({ selector: 'app-productos-menu', standalone: true,
  imports: [CommonModule, FormsModule, ConfirmarDirective, FormularioDirective, VentanaModalComponent, ImagenMenuFormularioComponent, IconoComponent],
  templateUrl: './productos-menu.component.html', styleUrl: '../restaurante.css' })
export class ProductosMenuComponent implements OnInit, OnChanges {
  readonly idRestaurante = input.required<number>();
  readonly revisionCarga = input(0);
  readonly productos = signal<ProductoMenu[]>([]);
  readonly categorias = signal<CategoriaMenu[]>([]);
  readonly cargando = signal(false);
  readonly ocupado = signal(false);
  readonly editor = signal(false);
  readonly errorCarga = signal('');
  readonly error = signal('');
  readonly busqueda = signal('');
  readonly filtroCategoria = signal<number | null>(null);
  readonly filtrados = computed(() => this.productos().filter(p => p.nombre.toLocaleLowerCase().includes(this.busqueda().trim().toLocaleLowerCase()) && (!this.filtroCategoria() || p.idCategoria === this.filtroCategoria())));
  private readonly api = inject(RestauranteService);
  private consultando = false;
  private readonly avisos = inject(NotificacionesService);
  private readonly destruir = inject(DestroyRef);
  readonly revision = signal(Date.now());
  idEdicion: number | null = null;
  imagenActual: string | null = null;
  imagen: SeleccionImagenMenu = { archivo: null, quitar: false, valida: true };
  formulario: ProductoMenuDatos = this.nuevo();
  nuevo(): ProductoMenuDatos { return { idCategoria: null, nombre: '', descripcion: '', precio: null, tiempoPreparacionMin: 20, disponible: true }; }
  ngOnInit(): void { this.cargar(); }
  ngOnChanges(cambios: SimpleChanges): void { if (cambios['revisionCarga'] && !cambios['revisionCarga'].firstChange) this.cargar(true); }
  cargar(silencioso = false): void {
    if (this.consultando || (silencioso && (this.ocupado() || this.editor()))) return;
    this.consultando = true;
    if (!silencioso) this.cargando.set(true); this.errorCarga.set('');
    forkJoin({ productos: this.api.productos(this.idRestaurante()), categorias: this.api.categorias(this.idRestaurante()) })
      .pipe(takeUntilDestroyed(this.destruir), finalize(() => { this.cargando.set(false); this.consultando = false; })).subscribe({
        next: datos => { this.productos.set(datos.productos); this.categorias.set(datos.categorias); this.revision.set(Date.now()); },
        error: error => this.errorCarga.set(mensajeErrorApi(error, 'No se pudo cargar la carta.')),
      });
  }
  foto(producto: ProductoMenu): string { const url = imagenUrl(producto.imagenUrl); return url ? url + (url.includes('?') ? '&' : '?') + 'v=' + this.revision() : '/images/producto-placeholder.svg'; }
  abrir(producto?: ProductoMenu): void {
    this.idEdicion = producto?.idProducto ?? null;
    this.imagenActual = producto?.imagenUrl ?? null;
    this.imagen = { archivo: null, quitar: false, valida: true };
    this.formulario = producto ? { idCategoria: producto.idCategoria, nombre: producto.nombre, descripcion: producto.descripcion ?? '', precio: producto.precio, tiempoPreparacionMin: producto.tiempoPreparacionMin, disponible: producto.disponible } : this.nuevo();
    this.error.set(''); this.editor.set(true);
  }
  cerrar(): void { if (!this.ocupado()) this.editor.set(false); }
  guardar(): void {
    if (this.ocupado() || !this.imagen.valida) return;
    this.ocupado.set(true); this.error.set('');
    const datos = { ...this.formulario, nombre: this.formulario.nombre.trim(), imagenUrl: this.productos().find(p => p.idProducto === this.idEdicion)?.imagenUrl ?? null };
    const operacion = this.idEdicion ? this.api.editarProducto(this.idRestaurante(), this.idEdicion, datos) : this.api.crearProducto(this.idRestaurante(), datos);
    let datosGuardados = false;
    operacion.pipe(
      tap(guardado => { this.idEdicion = guardado.idProducto; datosGuardados = true; }),
      switchMap(guardado => {
        if (this.imagen.archivo) return this.api.subirFoto('productos', guardado.idProducto, this.imagen.archivo).pipe(map(() => guardado));
        if (this.imagen.quitar && guardado.imagenUrl) return this.api.quitarFoto('productos', guardado.idProducto).pipe(map(() => guardado));
        return of(guardado);
      }),
      takeUntilDestroyed(this.destruir), finalize(() => this.ocupado.set(false)),
    ).subscribe({
      next: () => { this.editor.set(false); this.cargar(); this.avisos.mostrar('exito', 'Producto guardado correctamente.'); },
      error: error => this.error.set((datosGuardados ? 'Los datos del producto se conservaron. No se pudo guardar la imagen; puedes volver a guardar para reintentarlo. ' : '') + mensajeErrorApi(error, 'No se pudo guardar el producto.')),
    });
  }
  disponibilidad(producto: ProductoMenu): void {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.api.disponibilidad(this.idRestaurante(), producto.idProducto, !producto.disponible).pipe(takeUntilDestroyed(this.destruir), finalize(() => this.ocupado.set(false))).subscribe({
      next: () => { this.cargar(); this.avisos.mostrar('exito', 'Disponibilidad actualizada.'); },
      error: error => this.avisos.mostrar('error', mensajeErrorApi(error, 'No se pudo actualizar la disponibilidad.')),
    });
  }
}
