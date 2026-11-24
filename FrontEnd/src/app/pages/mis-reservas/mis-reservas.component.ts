import { PublicNavbarComponent } from '../../shared/public-navbar/public-navbar.component';
import { ComprobanteComponent } from '../../shared/comprobante/comprobante.component';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  BehaviorSubject,
  catchError,
  finalize,
  of,
  switchMap,
  timer,
  expand,
  reduce,
  map,
  EMPTY,
} from 'rxjs';
import {
  ESTADOS_RESERVA,
  GrupoReservasCliente,
  MiReservaResponse,
  MisReservasPaginaResponse,
} from '../../models/reserva.model';
import { ReservasService } from '../../services/reservas.service';
import { PagoModalComponent } from '../../shared/pago-modal/pago-modal.component';

@Component({
  selector: 'app-mis-reservas',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    PagoModalComponent,
    ComprobanteComponent,
    PublicNavbarComponent,
  ],
  templateUrl: './mis-reservas.component.html',
  styleUrl: './mis-reservas.component.css',
})
export class MisReservasComponent {
  private readonly service = inject(ReservasService);
  private readonly consultas = new BehaviorSubject({
    grupo: 'TODAS' as GrupoReservasCliente,
    page: 0,
    codigo: '',
    cabana: '',
    desde: '',
    hasta: '',
    espera: 0,
    periodo: 'PROXIMAS' as 'PROXIMAS' | 'PASADAS' | null,
  });
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('detalleDialog');

  readonly whatsappUrl = 'https://wa.me/51902897886';
  readonly periodo = signal<'PROXIMAS' | 'PASADAS' | null>('PROXIMAS');
  readonly grupo = signal<GrupoReservasCliente>('TODAS');
  readonly pagina = signal(0);
  readonly datos = signal<MisReservasPaginaResponse | null>(null);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly detalle = signal<MiReservaResponse | null>(null);
  readonly estados = ESTADOS_RESERVA;
  readonly filtros: { valor: GrupoReservasCliente; etiqueta: string }[] = [
    { valor: 'TODAS', etiqueta: 'Todas' },
    { valor: 'PENDIENTES', etiqueta: 'Pendientes' },
    { valor: 'CONFIRMADAS', etiqueta: 'Confirmadas' },
    { valor: 'EN_ESTANCIA', etiqueta: 'En estancia' },
    { valor: 'FINALIZADAS', etiqueta: 'Finalizadas' },
  ];
  codigo = '';
  cabana = '';
  desde = '';
  hasta = '';

  constructor() {
    this.consultas
      .pipe(
        switchMap((consulta) => {
          this.cargando.set(true);
          this.error.set('');
          this.datos.set(null);
          if (consulta.desde && consulta.hasta && consulta.desde > consulta.hasta) {
            this.error.set('La fecha desde debe ser anterior o igual a hasta.');
            this.cargando.set(false);
            return of(null);
          }
          return (consulta.espera ? timer(consulta.espera) : of(0)).pipe(
            switchMap(() => {
              if (!consulta.periodo)
                return this.service.consultarMisReservas(consulta.grupo, consulta.page, consulta);
              return this.service.consultarMisReservas(consulta.grupo, 0, consulta).pipe(
                expand((p) =>
                  p.page + 1 < p.totalPages
                    ? this.service.consultarMisReservas(consulta.grupo, p.page + 1, consulta)
                    : EMPTY,
                ),
                reduce((reservas, p) => reservas.concat(p.content), [] as MiReservaResponse[]),
                map((reservas) => {
                  const ahora = Date.now();
                  const seleccionadas = reservas.filter((r) => {
                    const salida = Date.parse(
                      r.fechaSalida + 'T' + (r.horaSalida || '13:00:00') + '-05:00',
                    );
                    return consulta.periodo === 'PASADAS' ? salida < ahora : salida >= ahora;
                  });
                  return {
                    content: seleccionadas.slice(consulta.page * 20, (consulta.page + 1) * 20),
                    page: consulta.page,
                    size: 20,
                    totalElements: seleccionadas.length,
                    totalPages: Math.ceil(seleccionadas.length / 20),
                  };
                }),
              );
            }),
            catchError((error: unknown) => {
              this.error.set(
                error instanceof HttpErrorResponse && error.status === 401
                  ? 'Tu sesión expiró. Inicia sesión nuevamente.'
                  : 'No se pudieron cargar tus reservas. Intenta nuevamente.',
              );
              return of(null);
            }),
            finalize(() => this.cargando.set(false)),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((datos) => this.datos.set(datos));
  }

  cambiarPeriodo(periodo: 'PROXIMAS' | 'PASADAS'): void {
    this.periodo.set(periodo);
    this.grupo.set('TODAS');
    this.pagina.set(0);
    this.cargar();
  }

  filtrar(grupo: GrupoReservasCliente): void {
    this.periodo.set(null);
    this.grupo.set(grupo);
    this.pagina.set(0);
    this.cargar();
  }

  cargar(espera = 0): void {
    this.consultas.next({
      grupo: this.grupo(),
      page: this.pagina(),
      codigo: this.codigo,
      cabana: this.cabana,
      desde: this.desde,
      hasta: this.hasta,
      espera,
      periodo: this.periodo(),
    });
  }

  actualizarFiltros(espera = 0): void {
    this.pagina.set(0);
    this.cargar(espera);
  }
  limpiarFiltros(): void {
    this.codigo = '';
    this.cabana = '';
    this.desde = '';
    this.hasta = '';
    this.grupo.set('TODAS');
    this.actualizarFiltros();
  }

  cambiarPagina(page: number): void {
    if (this.cargando() || page < 0 || page >= (this.datos()?.totalPages ?? 0)) return;
    this.pagina.set(page);
    this.cargar();
  }

  noches(r: MiReservaResponse): number {
    return Math.round(
      (Date.parse(r.fechaSalida + 'T00:00:00Z') - Date.parse(r.fechaIngreso + 'T00:00:00Z')) /
        86400000,
    );
  }

  pagado(r: MiReservaResponse): number {
    return r.estadoReserva === 'PENDIENTE_PAGO' || r.estadoReserva === 'EXPIRADA'
      ? 0
      : Math.round((r.montoTotal - r.saldoPendiente) * 100) / 100;
  }

  ocultarImagen(event: Event): void {
    (event.target as HTMLImageElement).hidden = true;
  }

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
