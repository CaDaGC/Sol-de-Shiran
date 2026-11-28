import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { Restaurante } from '../../../models/restaurante.model';
import { RestauranteService } from '../../../services/restaurante.service';
import { ActualizacionAutomaticaService } from '../../../services/actualizacion-automatica.service';
import { mensajeErrorApi } from '../../../core/error-api';
import { ListaRestaurantesComponent } from './lista-restaurantes.component';
import { CategoriasMenuComponent } from './categorias-menu.component';
import { ProductosMenuComponent } from './productos-menu.component';
import { EncargadosRestauranteComponent } from './encargados-restaurante.component';

export type SeccionRestaurante = 'restaurantes' | 'categorias-menu' | 'productos-menu' | 'encargados';
@Component({ selector: 'app-gestion-restaurantes', standalone: true,
  imports: [FormsModule, ListaRestaurantesComponent, CategoriasMenuComponent, ProductosMenuComponent, EncargadosRestauranteComponent],
  templateUrl: './gestion-restaurantes.component.html', styleUrl: '../restaurante.css' })
export class GestionRestaurantesComponent implements OnInit {
  readonly locales = signal<Restaurante[]>([]);
  readonly revisionCarga = signal(0);
  readonly seccion = signal<SeccionRestaurante>('restaurantes');
  readonly idLocal = signal<number | null>(null);
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly titulo = computed(() => ({ restaurantes: 'Restaurantes', 'categorias-menu': 'Categorías del menú', 'productos-menu': 'Productos del menú', encargados: 'Encargados de restaurante' })[this.seccion()]);
  readonly localesSeleccionados = computed(() => this.locales().filter(local => local.idRestaurante === this.idLocal()));
  private readonly api = inject(RestauranteService);
  private readonly automatico = inject(ActualizacionAutomaticaService);
  private consultando = false;
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destruir = inject(DestroyRef);
  ngOnInit(): void {
    this.ruta.queryParamMap.pipe(takeUntilDestroyed(this.destruir)).subscribe(params => {
      const seccion = params.get('seccion');
      this.seccion.set(['restaurantes', 'categorias-menu', 'productos-menu', 'encargados'].includes(seccion ?? '') ? seccion as SeccionRestaurante : 'restaurantes');
      const id = Number(params.get('local')); if (this.locales().some(local => local.idRestaurante === id)) this.idLocal.set(id);
    });
    this.cargar();
    this.automatico.observar().pipe(takeUntilDestroyed(this.destruir)).subscribe(() => this.cargar(true));
  }
  cargar(silencioso = false): void {
    if (this.consultando) return; this.consultando = true;
    if (!silencioso) this.cargando.set(true); this.error.set('');
    this.api.listar().pipe(takeUntilDestroyed(this.destruir), finalize(() => { this.cargando.set(false); this.consultando = false; })).subscribe({
      next: locales => { this.locales.set(locales); this.revisionCarga.update(valor => valor + 1); const id = Number(this.ruta.snapshot.queryParamMap.get('local')) || this.idLocal(); this.idLocal.set(locales.some(local => local.idRestaurante === id) ? id : locales[0]?.idRestaurante ?? null); },
      error: error => this.error.set(mensajeErrorApi(error, 'No se pudieron cargar los restaurantes.')),
    });
  }
  seleccionar(id: number): void { this.idLocal.set(id); void this.router.navigate([], { relativeTo: this.ruta, queryParams: { local: id }, queryParamsHandling: 'merge' }); }
}
