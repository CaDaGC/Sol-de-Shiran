import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { FrecuenciaCabana, IngresoDia, MetricasService, ResumenIngresos, ResumenReservas } from '../../services/metricas.service';

function fechaLocal(fecha: Date): string {
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
}

function fechaUtcValida(fecha: string): number | null {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(fecha)) return null;
  const dia = Date.parse(`${fecha}T00:00:00Z`);
  return Number.isFinite(dia) && new Date(dia).toISOString().slice(0, 10) === fecha ? dia : null;
}

export function rangoMetricasValido(desde: string, hasta: string): boolean {
  const inicio = fechaUtcValida(desde);
  const fin = fechaUtcValida(hasta);
  return inicio !== null && fin !== null && fin >= inicio && fin - inicio <= 366 * 86400000;
}

@Component({
  selector: 'app-metricas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './metricas.component.html',
  styleUrl: './metricas.component.css'
})
export class MetricasComponent implements OnDestroy {
  private readonly servicio = inject(MetricasService);
  private solicitud?: Subscription;
  desde = '';
  hasta = '';
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly ingresos = signal<ResumenIngresos | null>(null);
  readonly frecuencia = signal<ResumenReservas | null>(null);
  readonly nombres = signal(new Map<number, string>());
  readonly diaSeleccionado = signal<string | null>(null);
  readonly cabanaSeleccionada = signal<number | null>(null);
  readonly dias = computed(() => {
    const datos = this.ingresos();
    if (!datos) return [];
    const porFecha = new Map(datos.ingresosPorDia.map(dia => [dia.fecha, dia]));
    const serie: IngresoDia[] = [];
    const cursor = new Date(`${datos.desde}T12:00:00`);
    const fin = new Date(`${datos.hasta}T12:00:00`);
    while (cursor <= fin) {
      const fecha = fechaLocal(cursor);
      serie.push(porFecha.get(fecha) ?? { fecha, total: 0, operaciones: 0 });
      cursor.setDate(cursor.getDate() + 1);
    }
    return serie;
  });
  readonly mayorIngreso = computed(() => Math.max(1, ...this.dias().map(dia => dia.total)));
  readonly mayorFrecuencia = computed(() => Math.max(1, ...(this.frecuencia()?.ranking.map(r => r.reservas) ?? [])));
  readonly detalleDia = computed(() => this.dias().find(d => d.fecha === this.diaSeleccionado()) ?? null);
  readonly detalleCabana = computed(() => this.frecuencia()?.ranking.find(r => r.idCabana === this.cabanaSeleccionada()) ?? null);

  constructor() { this.ultimos(30); }

  ultimos(cantidad: number): void {
    const fin = new Date();
    const inicio = new Date(fin);
    inicio.setDate(fin.getDate() - cantidad + 1);
    this.desde = fechaLocal(inicio);
    this.hasta = fechaLocal(fin);
    this.cargar();
  }

  cargar(): void {
    if (!rangoMetricasValido(this.desde, this.hasta)) {
      this.solicitud?.unsubscribe();
      this.cargando.set(false);
      this.ingresos.set(null);
      this.frecuencia.set(null);
      this.diaSeleccionado.set(null);
      this.cabanaSeleccionada.set(null);
      this.error.set('Elige un rango válido de hasta 367 días.');
      return;
    }
    this.solicitud?.unsubscribe();
    this.cargando.set(true);
    this.error.set('');
    this.ingresos.set(null);
    this.frecuencia.set(null);
    this.solicitud = this.servicio.consultar(this.desde, this.hasta).subscribe({
      next: ({ ingresos, frecuencia, cabanas }) => {
        this.ingresos.set(ingresos);
        this.frecuencia.set(frecuencia);
        this.nombres.set(new Map(cabanas.map(c => [c.idCabana, c.nombre])));
        this.diaSeleccionado.set(null);
        this.cabanaSeleccionada.set(null);
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.cargando.set(false);
        this.error.set(error instanceof HttpErrorResponse && error.status === 403
          ? 'Solo un administrador puede consultar estas métricas.'
          : 'No se pudieron cargar las métricas. Comprueba los servicios y vuelve a intentar.');
      }
    });
  }

  altura(dia: IngresoDia): number { return dia.total === 0 ? 2 : Math.max(5, dia.total / this.mayorIngreso() * 100); }
  ancho(item: FrecuenciaCabana): number { return item.reservas / this.mayorFrecuencia() * 100; }
  nombre(id: number): string { return this.nombres().get(id) ?? `Cabaña #${id}`; }
  ngOnDestroy(): void { this.solicitud?.unsubscribe(); }
}
