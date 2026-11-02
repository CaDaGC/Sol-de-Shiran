import { Injectable } from '@angular/core';
import {
  HttpClient,
  HttpParams
} from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { API_CONFIG } from '../core/api.config';
import {
  CotizacionReservaResponse,
  CrearReservaRequest,
  FechasOcupadasResponse,
  ReservaResponse
} from '../models/reserva.model';

@Injectable({
  providedIn: 'root'
})
export class ReservasService {
  private readonly url = `${API_CONFIG.baseUrl}/reservas`;

  constructor(private readonly http: HttpClient) {}

  consultarDisponibilidad(
    idCabana: number,
    fechaIngreso: string,
    fechaSalida: string,
    cantidadHuespedes: number
  ): Observable<CotizacionReservaResponse> {
    const params = new HttpParams()
      .set('fechaIngreso', fechaIngreso)
      .set('fechaSalida', fechaSalida)
      .set('cantidadHuespedes', cantidadHuespedes);

    return this.http
      .get<CotizacionReservaResponse>(
        `${this.url}/cabanas/${idCabana}/disponibilidad`,
        { params }
      )
      .pipe(timeout({ first: 15000 }));
  }

  consultarFechasOcupadas(
    idCabana: number,
    desde: string,
    hasta: string
  ): Observable<FechasOcupadasResponse> {
    const params = new HttpParams()
      .set('desde', desde)
      .set('hasta', hasta);

    return this.http
      .get<FechasOcupadasResponse>(
        `${this.url}/cabanas/${idCabana}/fechas-ocupadas`,
        { params }
      )
      .pipe(timeout({ first: 15000 }));
  }

  crearReservaTemporal(
    request: CrearReservaRequest
  ): Observable<ReservaResponse> {
    return this.http
      .post<ReservaResponse>(this.url, request)
      .pipe(timeout({ first: 15000 }));
  }
}
