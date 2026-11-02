import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Cabana } from '../../models/cabana.model';
import { CabanaService } from '../../services/cabana.service';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './catalogo.component.html',
  styleUrl: './catalogo.component.css'
})
export class CatalogoComponent implements OnInit {
  readonly cabanas = signal<Cabana[]>([]);
  readonly cargando = signal(true);
  readonly mensajeError = signal('');

  private readonly platformId = inject(PLATFORM_ID);

  constructor(private readonly cabanaService: CabanaService) {}

  ngOnInit(): void {
    // Evita hacer la petición al API durante el prerenderizado del servidor.
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
          'No pudimos cargar las cabañas. Verifica que el Gateway y el microservicio estén activos.'
        );
        this.cargando.set(false);
      }
    });
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
