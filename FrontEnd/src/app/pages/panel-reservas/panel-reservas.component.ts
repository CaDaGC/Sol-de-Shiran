import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { BehaviorSubject, catchError, finalize, of, switchMap, timer } from 'rxjs';
import { ESTADOS_RESERVA, ReservaPanelResponse, ReservasPaginaResponse } from '../../models/reserva.model';
import { ReservaDetalleComponent } from '../../shared/reserva-detalle/reserva-detalle.component';
import { ReservasService } from '../../services/reservas.service';

@Component({
  selector: 'app-panel-reservas',
  standalone: true,
  imports: [CommonModule, FormsModule, ReservaDetalleComponent],
  templateUrl: './panel-reservas.component.html',
  styleUrl: './panel-reservas.component.css'
})
export class PanelReservasComponent {
  private readonly reservasService = inject(ReservasService);
  private readonly consultas = new BehaviorSubject({ buscar: '', idCabana: null as number | null, page: 0, espera: 0 });

  readonly resultado = signal<ReservasPaginaResponse | null>(null);
  readonly cabanas = signal<{ idCabana: number; nombre: string }[]>([]);
  readonly cargando = signal(true);
  readonly cargandoCabanas = signal(true);
  readonly error = signal('');
  readonly errorCabanas = signal('');
  readonly buscar = signal('');
  readonly idCabana = signal<number | null>(null);
  readonly pagina = signal(0);
  readonly idDetalle = signal<number | null>(null);
  readonly idSeleccionada = signal<number | null>(null);
  readonly tamanoPagina = 20;
  readonly reservas = computed(() => this.resultado()?.content ?? []);
  readonly seleccionada = computed(() => this.reservas().find(r => r.idReserva === this.idSeleccionada())
    ?? this.reservas()[0] ?? null);

  noches(reserva: ReservaPanelResponse): number {
    return Math.round((Date.parse(reserva.fechaSalida + 'T00:00:00Z')
      - Date.parse(reserva.fechaIngreso + 'T00:00:00Z')) / 86400000);
  }

  pagado(reserva: ReservaPanelResponse): number {
    return Math.round((reserva.montoTotal - reserva.saldoPendiente) * 100) / 100;
  }

  readonly hayFiltros = computed(() => !!this.buscar().trim() || this.idCabana() !== null);

  constructor() {
    this.reservasService.listarCabanasDelPanel().pipe(takeUntilDestroyed()).subscribe({
      next: cabanas => {
        this.cabanas.set(cabanas);
        this.cargandoCabanas.set(false);
      },
      error: () => {
        this.errorCabanas.set('No se pudo cargar el selector de cabañas. Recarga la página para intentarlo de nuevo.');
        this.cargandoCabanas.set(false);
      }
    });

    this.consultas.pipe(
      switchMap(consulta => {
        this.cargando.set(true);
        this.error.set('');
        this.resultado.set(null);
        return timer(consulta.espera).pipe(
          switchMap(() => this.reservasService.listarReservas(
            consulta.buscar, consulta.idCabana, consulta.page, this.tamanoPagina)),
          catchError((error: unknown) => {
            this.error.set(error instanceof HttpErrorResponse && error.status === 401
              ? 'Tu sesión expiró. Vuelve a iniciar sesión.'
              : error instanceof HttpErrorResponse && error.status === 403
                ? 'Tu cuenta no tiene permiso para consultar las reservas.'
                : 'No se pudieron cargar las reservas. Intenta nuevamente.');
            return of(null);
          }),
          finalize(() => this.cargando.set(false))
        );
      }),
      takeUntilDestroyed()
    ).subscribe(resultado => this.resultado.set(resultado));
  }

  readonly estados = ESTADOS_RESERVA;

  limpiarFiltros(): void {
    this.buscar.set('');
    this.idCabana.set(null);
    this.pagina.set(0);
    this.cargar();
  }

  actualizarBusqueda(valor: string): void {
    this.buscar.set(valor);
    this.pagina.set(0);
    this.cargar(300);
  }

  filtrarCabana(id: number | null): void {
    this.idCabana.set(id);
    this.pagina.set(0);
    this.cargar();
  }

  cambiarPagina(pagina: number): void {
    if (this.cargando() || pagina < 0 || pagina >= (this.resultado()?.totalPages ?? 0)) return;
    this.pagina.set(pagina);
    this.cargar();
  }

  cargar(espera = 0): void {
    this.consultas.next({ buscar: this.buscar(), idCabana: this.idCabana(), page: this.pagina(), espera });
  }
}
