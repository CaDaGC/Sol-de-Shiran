import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { afterNextRender, Component, DestroyRef, ElementRef, inject, input, OnInit, output, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { ESTADOS_RESERVA, ReservaDetalleResponse } from '../../models/reserva.model';
import { ReservasService } from '../../services/reservas.service';

@Component({
  selector: 'app-reserva-detalle',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reserva-detalle.component.html',
  styleUrl: './reserva-detalle.component.css'
})
export class ReservaDetalleComponent implements OnInit {
  private readonly reservasService = inject(ReservasService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  readonly idReserva = input.required<number>();
  readonly cerrar = output<void>();
  readonly detalle = signal<ReservaDetalleResponse | null>(null);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly estados = ESTADOS_RESERVA;

  constructor() {
    afterNextRender(() => this.dialog().nativeElement.showModal());
  }

  ngOnInit(): void {
    this.cargarDetalle();
  }

  cargarDetalle(): void {
    this.cargando.set(true);
    this.error.set('');
    this.detalle.set(null);
    this.reservasService.consultarDetalle(this.idReserva()).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.cargando.set(false))
    ).subscribe({
      next: detalle => this.detalle.set(detalle),
      error: (error: unknown) => {
        const status = error instanceof HttpErrorResponse ? error.status : 0;
        this.error.set(status === 404 ? 'Esta reserva ya no está disponible.'
          : status === 401 ? 'Tu sesión expiró. Vuelve a iniciar sesión.'
          : status === 403 ? 'Tu cuenta no tiene permiso para consultar esta reserva.'
          : 'No se pudo cargar el detalle. Intenta nuevamente.');
      }
    });
  }

  cerrarModal(): void {
    this.dialog().nativeElement.close();
    this.cerrar.emit();
  }

  cerrarDesdeFondo(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement) this.cerrarModal();
  }
}
