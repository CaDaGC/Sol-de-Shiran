import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { forkJoin, timeout } from 'rxjs';
import { API_CONFIG } from '../core/api.config';

export interface IngresoDia { fecha: string; total: number; operaciones: number; }
export interface ResumenIngresos {
  desde: string; hasta: string; totalIngresos: number; numeroPagos: number;
  ingresosPorDia: IngresoDia[];
}
export interface FrecuenciaCabana { idCabana: number; reservas: number; }
export interface ResumenReservas {
  desde: string; hasta: string; totalReservas: number; ranking: FrecuenciaCabana[];
}
export interface CabanaGestion { idCabana: number; nombre: string; }

@Injectable({ providedIn: 'root' })
export class MetricasService {
  private readonly http = inject(HttpClient);
  private readonly url = API_CONFIG.baseUrl;

  consultar(desde: string, hasta: string) {
    const params = new HttpParams().set('desde', desde).set('hasta', hasta);
    return forkJoin({
      ingresos: this.http.get<ResumenIngresos>(`${this.url}/pagos/metricas/ingresos`, { params }),
      frecuencia: this.http.get<ResumenReservas>(`${this.url}/reservas/metricas/frecuencia`, { params }),
      cabanas: this.http.get<CabanaGestion[]>(`${this.url}/cabanas/gestion`)
    }).pipe(timeout({ first: 15000 }));
  }
}
