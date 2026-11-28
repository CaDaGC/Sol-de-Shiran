import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { ProductoMenu } from '../../../models/restaurante.model';
import { RestauranteService } from '../../../services/restaurante.service';
import { NotificacionesService } from '../../../services/notificaciones.service';
import { ActualizacionAutomaticaService } from '../../../services/actualizacion-automatica.service';
import { mensajeErrorApi } from '../../../core/error-api';
import { ConfirmarDirective } from '../../../shared/confirmar/confirmar.directive';

@Component({ selector: 'app-disponibilidad-carta', standalone: true, imports: [CommonModule, FormsModule, ConfirmarDirective],
  template: `<div class="barra-herramientas"><label class="form-label">Buscar en la carta<input class="form-control mt-2" type="search" placeholder="Producto o categoría" [ngModel]="busqueda()" (ngModelChange)="busqueda.set($event)" /></label></div>
  <p class="nota">Marca un producto como agotado cuando ya no pueda prepararse. Dejará de admitirse en pedidos nuevos. Los pedidos ya recibidos se conservan.</p>
  @if (cargando()) { <p role="status" class="mensaje-carga">Cargando carta…</p> }
  @else if (error()) { <div role="alert" class="alert alert-danger">{{ error() }}</div> }
  @else if (!filtrados().length) { <div class="vacio"><h2>Sin productos para mostrar</h2><p>Prueba otra búsqueda o solicita al administrador que registre la carta.</p></div> }
  @else { <div class="tabla-contenedor"><div class="table-responsive"><table class="table tabla-menu"><thead><tr><th>Producto</th><th>Categoría</th><th>Precio</th><th>Disponibilidad</th><th>Acciones</th></tr></thead><tbody>
  @for (producto of filtrados(); track producto.idProducto) { <tr><td><strong>{{ producto.nombre }}</strong></td><td>{{ producto.nombreCategoria }}</td><td>{{ producto.precio | currency:'PEN':'S/ ':'1.2-2' }}</td><td><span class="estado" [attr.data-estado]="producto.disponible ? 'ACTIVO' : 'INACTIVO'">{{ producto.disponible ? 'Disponible' : 'Agotado' }}</span></td><td><button type="button" class="btn btn-sm" [class.btn-outline-danger]="producto.disponible" [class.btn-outline-success]="!producto.disponible" [disabled]="ocupado()" [appConfirmar]="producto.disponible ? '¿Confirmas que este producto está agotado y debe dejar de recibirse en pedidos nuevos?' : '¿Confirmas que este producto vuelve a estar disponible?'" (click)="cambiar(producto)">{{ producto.disponible ? 'Marcar agotado' : 'Habilitar' }}</button></td></tr> }
  </tbody></table></div></div> }`, styleUrl: '../restaurante.css',
})
export class DisponibilidadCartaComponent implements OnInit {
  readonly idRestaurante = input.required<number>();
  readonly productos = signal<ProductoMenu[]>([]);
  readonly cargando = signal(false);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly busqueda = signal('');
  readonly filtrados = computed(() => this.productos().filter(p => (p.nombre + ' ' + p.nombreCategoria).toLocaleLowerCase().includes(this.busqueda().trim().toLocaleLowerCase())));
  private readonly api = inject(RestauranteService);
  private readonly automatico = inject(ActualizacionAutomaticaService);
  private consultando = false;
  private readonly avisos = inject(NotificacionesService);
  private readonly destruir = inject(DestroyRef);
  ngOnInit(): void { this.cargar(); this.automatico.observar().pipe(takeUntilDestroyed(this.destruir)).subscribe(() => this.cargar(true)); }
  cargar(silencioso = false): void {
    if (this.consultando || (silencioso && this.ocupado())) return; this.consultando = true;
    if (!silencioso) this.cargando.set(true); this.error.set('');
    this.api.cartaOperativa(this.idRestaurante()).pipe(takeUntilDestroyed(this.destruir), finalize(() => { this.cargando.set(false); this.consultando = false; })).subscribe({
      next: productos => this.productos.set(productos), error: error => this.error.set(mensajeErrorApi(error, 'No se pudo cargar la carta.')),
    });
  }
  cambiar(producto: ProductoMenu): void {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.api.disponibilidad(this.idRestaurante(), producto.idProducto, !producto.disponible).pipe(takeUntilDestroyed(this.destruir), finalize(() => this.ocupado.set(false))).subscribe({
      next: actualizado => { this.productos.update(lista => lista.map(p => p.idProducto === actualizado.idProducto ? actualizado : p)); this.avisos.mostrar('exito', 'Disponibilidad actualizada.'); },
      error: error => this.avisos.mostrar('error', mensajeErrorApi(error, 'No se pudo actualizar el producto.')),
    });
  }
}
