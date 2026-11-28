import { Component, DestroyRef, OnChanges, OnInit, SimpleChanges, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { CategoriaMenu, CategoriaMenuDatos } from '../../../models/restaurante.model';
import { RestauranteService } from '../../../services/restaurante.service';
import { NotificacionesService } from '../../../services/notificaciones.service';
import { mensajeErrorApi } from '../../../core/error-api';
import { ConfirmarDirective } from '../../../shared/confirmar/confirmar.directive';
import { FormularioDirective } from '../../../shared/formulario/formulario.directive';
import { VentanaModalComponent } from '../../../shared/ventana-modal/ventana-modal.component';
import { IconoComponent } from '../../../shared/icono/icono.component';

@Component({ selector: 'app-categorias-menu', standalone: true, imports: [FormsModule, ConfirmarDirective, FormularioDirective, VentanaModalComponent, IconoComponent],
  templateUrl: './categorias-menu.component.html', styleUrl: '../restaurante.css' })
export class CategoriasMenuComponent implements OnInit, OnChanges {
  readonly idRestaurante = input.required<number>();
  readonly revisionCarga = input(0);
  readonly categorias = signal<CategoriaMenu[]>([]);
  readonly cargando = signal(false);
  readonly ocupado = signal(false);
  readonly editor = signal(false);
  readonly errorCarga = signal('');
  readonly error = signal('');
  readonly busqueda = signal('');
  readonly filtradas = computed(() => this.categorias().filter(c => c.nombre.toLocaleLowerCase().includes(this.busqueda().trim().toLocaleLowerCase())));
  private readonly api = inject(RestauranteService);
  private consultando = false;
  private readonly avisos = inject(NotificacionesService);
  private readonly destruir = inject(DestroyRef);
  idEdicion: number | null = null;
  formulario: CategoriaMenuDatos = { nombre: '', tipo: 'COMIDA' };
  ngOnInit(): void { this.cargar(); }
  ngOnChanges(cambios: SimpleChanges): void { if (cambios['revisionCarga'] && !cambios['revisionCarga'].firstChange) this.cargar(true); }
  cargar(silencioso = false): void {
    if (this.consultando || (silencioso && (this.ocupado() || this.editor()))) return;
    this.consultando = true;
    if (!silencioso) this.cargando.set(true); this.errorCarga.set('');
    this.api.categorias(this.idRestaurante()).pipe(takeUntilDestroyed(this.destruir), finalize(() => { this.cargando.set(false); this.consultando = false; })).subscribe({
      next: datos => this.categorias.set(datos), error: error => this.errorCarga.set(mensajeErrorApi(error, 'No se pudieron cargar las categorías.')),
    });
  }
  abrir(categoria?: CategoriaMenu): void { this.idEdicion = categoria?.idCategoria ?? null; this.formulario = { nombre: categoria?.nombre ?? '', tipo: categoria?.tipo ?? 'COMIDA' }; this.error.set(''); this.editor.set(true); }
  cerrar(): void { if (!this.ocupado()) this.editor.set(false); }
  guardar(): void {
    if (this.ocupado()) return;
    this.ocupado.set(true); this.error.set('');
    const datos = { ...this.formulario, nombre: this.formulario.nombre.trim() };
    const operacion = this.idEdicion ? this.api.editarCategoria(this.idRestaurante(), this.idEdicion, datos) : this.api.crearCategoria(this.idRestaurante(), datos);
    operacion.pipe(takeUntilDestroyed(this.destruir), finalize(() => this.ocupado.set(false))).subscribe({
      next: () => { this.editor.set(false); this.cargar(); this.avisos.mostrar('exito', 'Categoría guardada correctamente.'); },
      error: error => this.error.set(mensajeErrorApi(error, 'No se pudo guardar la categoría.')),
    });
  }
  eliminar(categoria: CategoriaMenu): void {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.api.eliminarCategoria(this.idRestaurante(), categoria.idCategoria).pipe(takeUntilDestroyed(this.destruir), finalize(() => this.ocupado.set(false))).subscribe({
      next: () => { this.cargar(); this.avisos.mostrar('exito', 'Categoría eliminada.'); },
      error: error => this.avisos.mostrar('error', mensajeErrorApi(error, 'No se pudo eliminar la categoría.')),
    });
  }
}
