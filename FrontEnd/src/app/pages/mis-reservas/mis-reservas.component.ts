import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, finalize, of, switchMap } from 'rxjs';
import { ESTADOS_RESERVA, GrupoReservasCliente, MiReservaResponse, MisReservasPaginaResponse } from '../../models/reserva.model';
import { ReservasService } from '../../services/reservas.service';
import { PagoModalComponent } from '../../shared/pago-modal/pago-modal.component';

@Component({
  selector: 'app-mis-reservas',
  standalone: true,
  imports: [CommonModule, RouterLink, PagoModalComponent],
  templateUrl: './mis-reservas.component.html',
  styleUrl: './mis-reservas.component.css'
})
export class MisReservasComponent {
  private readonly service = inject(ReservasService);
  private readonly consultas = new BehaviorSubject({ grupo: 'TODAS' as GrupoReservasCliente, page: 0 });
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('detalleDialog');

  readonly grupo = signal<GrupoReservasCliente>('TODAS');
  readonly pagina = signal(0);
  readonly datos = signal<MisReservasPaginaResponse | null>(null);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly detalle = signal<MiReservaResponse | null>(null);
  readonly estados = ESTADOS_RESERVA;
  readonly filtros: { valor: GrupoReservasCliente; etiqueta: string }[] = [
    { valor: 'TODAS', etiqueta: 'Todas' }, { valor: 'PENDIENTES', etiqueta: 'Pendientes' },
    { valor: 'CONFIRMADAS', etiqueta: 'Confirmadas' }, { valor: 'PASADAS', etiqueta: 'Pasadas' }
  ];
  readonly activas = computed(() => this.datos()?.content.filter(r => r.grupo !== 'PASADAS') ?? []);
  readonly pasadas = computed(() => this.datos()?.content.filter(r => r.grupo === 'PASADAS') ?? []);

  constructor() {
    this.consultas.pipe(
      switchMap(consulta => {
        this.cargando.set(true);
        this.error.set('');
        this.datos.set(null);
        return this.service.consultarMisReservas(consulta.grupo, consulta.page).pipe(
          catchError((error: unknown) => {
            this.error.set(error instanceof HttpErrorResponse && error.status === 401
              ? 'Tu sesión expiró. Inicia sesión nuevamente.' : 'No se pudieron cargar tus reservas. Intenta nuevamente.');
            return of(null);
          }),
          finalize(() => this.cargando.set(false))
        );
      }), takeUntilDestroyed()
    ).subscribe(datos => this.datos.set(datos));
  }

  filtrar(grupo: GrupoReservasCliente): void {
    this.grupo.set(grupo);
    this.pagina.set(0);
    this.cargar();
  }

  cargar(): void {
    this.consultas.next({ grupo: this.grupo(), page: this.pagina() });
  }

  cambiarPagina(page: number): void {
    if (this.cargando() || page < 0 || page >= (this.datos()?.totalPages ?? 0)) return;
    this.pagina.set(page);
    this.cargar();
  }

  noches(r: MiReservaResponse): number {
    return Math.round((Date.parse(r.fechaSalida + 'T00:00:00Z') - Date.parse(r.fechaIngreso + 'T00:00:00Z')) / 86400000);
  }

  pagado(r: MiReservaResponse): number {
    return r.estadoReserva === 'PENDIENTE_PAGO' || r.estadoReserva === 'EXPIRADA'
      ? 0 : Math.round((r.montoTotal - r.saldoPendiente) * 100) / 100;
  }

  ocultarImagen(event: Event): void { (event.target as HTMLImageElement).hidden = true; }

  verDetalle(reserva: MiReservaResponse): void {
    this.detalle.set(reserva);
    this.dialog().nativeElement.showModal();
  }

  cerrarDetalle(): void {
    this.dialog().nativeElement.close();
    this.detalle.set(null);
  }

  cerrarDesdeFondo(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement) this.cerrarDetalle();
  }
}
