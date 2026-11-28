import { Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, map, of, switchMap, tap } from 'rxjs';
import { Restaurante, RestauranteDatos } from '../../../models/restaurante.model';
import { RestauranteService } from '../../../services/restaurante.service';
import { NotificacionesService } from '../../../services/notificaciones.service';
import { mensajeErrorApi } from '../../../core/error-api';
import { imagenUrl } from '../../../core/imagen-url';
import { ConfirmarDirective } from '../../../shared/confirmar/confirmar.directive';
import { FormularioDirective } from '../../../shared/formulario/formulario.directive';
import { VentanaModalComponent } from '../../../shared/ventana-modal/ventana-modal.component';
import { ImagenMenuFormularioComponent, SeleccionImagenMenu } from '../../../shared/imagen-menu-formulario/imagen-menu-formulario.component';
import { IconoComponent } from '../../../shared/icono/icono.component';

@Component({ selector: 'app-lista-restaurantes', standalone: true,
  imports: [FormsModule, ConfirmarDirective, FormularioDirective, VentanaModalComponent, ImagenMenuFormularioComponent, IconoComponent],
  templateUrl: './lista-restaurantes.component.html', styleUrl: '../restaurante.css',
})
export class ListaRestaurantesComponent {
  readonly locales = input.required<Restaurante[]>();
  readonly actualizado = output<void>();
  readonly busqueda = signal('');
  readonly estado = signal('');
  readonly filtrados = computed(() => this.locales().filter(local =>
    local.nombre.toLocaleLowerCase().includes(this.busqueda().trim().toLocaleLowerCase()) && (!this.estado() || local.estado === this.estado())));
  readonly editando = signal(false);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly revision = signal(Date.now());
  private readonly api = inject(RestauranteService);
  private readonly avisos = inject(NotificacionesService);
  private readonly destruir = inject(DestroyRef);
  idEdicion: number | null = null;
  imagenActual: string | null = null;
  imagen: SeleccionImagenMenu = { archivo: null, quitar: false, valida: true };
  formulario: RestauranteDatos = { nombre: '', especialidad: '' };
  abrir(local?: Restaurante): void {
    this.idEdicion = local?.idRestaurante ?? null;
    this.imagenActual = local?.imagenUrl ?? null;
    this.imagen = { archivo: null, quitar: false, valida: true };
    this.formulario = { nombre: local?.nombre ?? '', especialidad: local?.especialidad ?? '' };
    this.error.set(''); this.editando.set(true);
  }
  cerrar(): void { if (!this.ocupado()) this.editando.set(false); }
  foto(local: Restaurante): string { const url = imagenUrl(local.imagenUrl); return url ? url + (url.includes('?') ? '&' : '?') + 'v=' + this.revision() : '/images/restaurante-placeholder.svg'; }
  guardar(): void {
    if (this.ocupado() || !this.imagen.valida) return;
    this.ocupado.set(true); this.error.set('');
    const datos = { ...this.formulario, nombre: this.formulario.nombre.trim(), imagenUrl: this.locales().find(local => local.idRestaurante === this.idEdicion)?.imagenUrl ?? null };
    const operacion = this.idEdicion ? this.api.editar(this.idEdicion, datos) : this.api.crear(datos);
    let datosGuardados = false;
    operacion.pipe(
      tap(guardado => { this.idEdicion = guardado.idRestaurante; datosGuardados = true; }),
      switchMap(guardado => {
        if (this.imagen.archivo) return this.api.subirFoto('locales', guardado.idRestaurante, this.imagen.archivo).pipe(map(() => guardado));
        if (this.imagen.quitar && guardado.imagenUrl) return this.api.quitarFoto('locales', guardado.idRestaurante).pipe(map(() => guardado));
        return of(guardado);
      }),
      takeUntilDestroyed(this.destruir), finalize(() => this.ocupado.set(false)),
    ).subscribe({
      next: () => { this.editando.set(false); this.actualizado.emit(); this.avisos.mostrar('exito', 'Restaurante guardado correctamente.'); },
      error: error => this.error.set((datosGuardados ? 'Los datos del restaurante se conservaron. No se pudo guardar la imagen; puedes volver a guardar para reintentarlo. ' : '') + mensajeErrorApi(error, 'No se pudo guardar el restaurante.')),
    });
  }
  cambiarEstado(local: Restaurante): void {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.api.cambiarEstado(local.idRestaurante, local.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO')
      .pipe(takeUntilDestroyed(this.destruir), finalize(() => this.ocupado.set(false))).subscribe({
        next: () => { this.actualizado.emit(); this.avisos.mostrar('exito', 'Estado del restaurante actualizado.'); },
        error: error => this.avisos.mostrar('error', mensajeErrorApi(error, 'No se pudo actualizar el restaurante.')),
      });
  }
}
