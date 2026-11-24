import { FormularioDirective } from '../../shared/formulario/formulario.directive';
import { CommonModule, isPlatformBrowser, DOCUMENT } from '@angular/common';
import {
  Component,
  afterRenderEffect,
  OnInit,
  PLATFORM_ID,
  inject,
  signal,
  computed,
  DestroyRef,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { toSignal, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { from, mergeMap, toArray, map, Subscription } from 'rxjs';
import { ReservasService } from '../../services/reservas.service';
import { PublicNavbarComponent } from '../../shared/public-navbar/public-navbar.component';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Cabana } from '../../models/cabana.model';
import { CabanaService } from '../../services/cabana.service';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [FormularioDirective, CommonModule, RouterLink, FormsModule, PublicNavbarComponent],
  templateUrl: './catalogo.component.html',
  styleUrl: './catalogo.component.css',
})
export class CatalogoComponent implements OnInit {
  private readonly documento = inject(DOCUMENT);
  private readonly fragmento = toSignal(inject(ActivatedRoute).fragment);
  readonly cabanas = signal<Cabana[]>([]);
  readonly cargando = signal(true);
  readonly mensajeError = signal('');

  readonly buscando = signal(false);
  readonly filtrado = signal(false);
  readonly disponibles = signal<Cabana[]>([]);
  readonly visibles = computed(() => (this.filtrado() ? this.disponibles() : this.cabanas()));
  readonly fechaMinima = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Lima',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date());
  fechaIngreso = '';
  fechaSalida = '';
  huespedes = 2;
  errorBusqueda = '';
  parametros: Record<string, string | number> = {};
  private consulta?: Subscription;
  private readonly reservas = inject(ReservasService);
  private readonly destroyRef = inject(DestroyRef);

  buscarDisponibilidad(): void {
    if (
      !this.fechaIngreso ||
      !this.fechaSalida ||
      this.fechaIngreso < this.fechaMinima ||
      this.fechaSalida <= this.fechaIngreso
    ) {
      this.errorBusqueda = 'Selecciona una llegada válida y una salida posterior.';
      return;
    }
    if (!Number.isInteger(this.huespedes) || this.huespedes < 1) {
      this.errorBusqueda = 'Indica al menos un huésped.';
      return;
    }
    this.consulta?.unsubscribe();
    this.errorBusqueda = '';
    this.buscando.set(true);
    const entrada = this.fechaIngreso,
      salida = this.fechaSalida,
      cantidad = this.huespedes;
    this.consulta = from(this.cabanas().filter((c) => c.aforoMaximo >= cantidad))
      .pipe(
        mergeMap(
          (c) =>
            this.reservas
              .consultarDisponibilidad(c.idCabana, entrada, salida, cantidad)
              .pipe(map((resultado) => ({ c, disponible: resultado.disponible }))),
          4,
        ),
        toArray(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (resultados) => {
          const ids = new Set(resultados.filter((r) => r.disponible).map((r) => r.c.idCabana));
          this.disponibles.set(this.cabanas().filter((c) => ids.has(c.idCabana)));
          this.parametros = {
            fechaIngreso: entrada,
            fechaSalida: salida,
            adultos: cantidad,
            ninos: 0,
          };
          this.filtrado.set(true);
          this.buscando.set(false);
        },
        error: () => {
          this.buscando.set(false);
          this.errorBusqueda = 'No pudimos consultar la disponibilidad. Inténtalo nuevamente.';
        },
      });
  }

  volverACabanas(): void {
    this.documento.getElementById('cabanas')?.scrollIntoView({ block: 'start' });
  }

  limpiarBusqueda(): void {
    this.consulta?.unsubscribe();
    this.buscando.set(false);
    this.filtrado.set(false);
    this.parametros = {};
    this.errorBusqueda = '';
  }

  private readonly platformId = inject(PLATFORM_ID);

  constructor(private readonly cabanaService: CabanaService) {
    afterRenderEffect(() => {
      const fragmento = this.fragmento();
      if (
        !this.cargando() &&
        fragmento &&
        ['naturaleza', 'cabanas', 'contacto'].includes(fragmento)
      ) {
        this.documento.getElementById(fragmento)?.scrollIntoView({ block: 'start' });
      }
    });
  }

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.cargarCabanas();
    }
  }

  cargarCabanas(): void {
    this.cargando.set(true);
    this.mensajeError.set('');

    this.cabanaService.listarCabanas().subscribe({
      next: (cabanas) => {
        this.cabanas.set(cabanas);
        this.cargando.set(false);
      },
      error: (error) => {
        console.error('No se pudo cargar el catálogo de cabañas.', error);
        this.mensajeError.set(
          'No pudimos cargar las cabañas. Inténtalo nuevamente en unos momentos.',
        );
        this.cargando.set(false);
      },
    });
  }

  formatearTarifa(tarifa: number): string {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(tarifa);
  }

  mostrarImagenAlternativa(event: Event): void {
    const imagen = event.target as HTMLImageElement;

    if (imagen.dataset['alternativaUsada'] !== 'true') {
      imagen.dataset['alternativaUsada'] = 'true';
      imagen.src = '/images/cabana-placeholder.svg';
    }
  }
}
