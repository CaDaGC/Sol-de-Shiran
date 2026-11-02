import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CabanaDetalle } from '../../models/cabana.model';
import { CabanaService } from '../../services/cabana.service';

@Component({
  selector: 'app-detalle-cabana',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './detalle-cabana.component.html',
  styleUrl: './detalle-cabana.component.css'
})
export class DetalleCabanaComponent implements OnInit {
  readonly cabana = signal<CabanaDetalle | null>(null);
  readonly cargando = signal(true);
  readonly mensajeError = signal('');
  readonly indiceImagen = signal(0);

  readonly imagenes = computed(() => {
    const detalle = this.cabana();

    if (!detalle) {
      return [];
    }

    return [
      detalle.imagenPrincipal,
      ...(detalle.imagenesSecundarias ?? [])
    ].filter((url) => Boolean(url));
  });

  readonly imagenActual = computed(
    () => this.imagenes()[this.indiceImagen()] ?? ''
  );

  private readonly platformId = inject(PLATFORM_ID);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly cabanaService: CabanaService
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.route.paramMap.subscribe((params) => {
      const id = Number(params.get('id'));

      if (!Number.isInteger(id) || id <= 0) {
        this.mensajeError.set('El identificador de la cabaña no es válido.');
        this.cargando.set(false);
        return;
      }

      this.cargarDetalle(id);
    });
  }

  private cargarDetalle(id: number): void {
    this.cargando.set(true);
    this.mensajeError.set('');
    this.cabana.set(null);
    this.indiceImagen.set(0);

    this.cabanaService.obtenerDetalle(id).subscribe({
      next: (detalle) => {
        this.cabana.set(detalle);
        this.cargando.set(false);
      },
      error: (error) => {
        console.error('No se pudo cargar el detalle de la cabaña.', error);
        this.mensajeError.set(
          'No se encontró la cabaña o no se pudo cargar su información.'
        );
        this.cargando.set(false);
      }
    });
  }

  seleccionarImagen(indice: number): void {
    if (indice >= 0 && indice < this.imagenes().length) {
      this.indiceImagen.set(indice);
    }
  }

  imagenAnterior(): void {
    const total = this.imagenes().length;

    if (total > 1) {
      this.indiceImagen.update((indice) => (indice - 1 + total) % total);
    }
  }

  imagenSiguiente(): void {
    const total = this.imagenes().length;

    if (total > 1) {
      this.indiceImagen.update((indice) => (indice + 1) % total);
    }
  }

  formatearTarifa(tarifa: number): string {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(tarifa);
  }

  mostrarImagenAlternativa(event: Event): void {
    const imagen = event.target as HTMLImageElement;

    if (imagen.dataset['alternativaUsada'] !== 'true') {
      imagen.dataset['alternativaUsada'] = 'true';
      imagen.src = 'https://placehold.co/900x600?text=Imagen+no+disponible';
    }
  }
}
