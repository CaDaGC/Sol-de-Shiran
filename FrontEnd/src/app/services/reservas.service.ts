import { Injectable } from '@angular/core';
import {
  HttpClient,
  HttpParams
} from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { API_CONFIG } from '../core/api.config';
import {
  CheckoutRequest,
  CheckoutResponse,
  CotizacionReservaResponse,
  CrearReservaRequest,
  FechasOcupadasResponse,
  ReservaResponse,
  ReservasPaginaResponse,
  ReservaDetalleResponse,
  GrupoReservasCliente,
  MisReservasPaginaResponse
} from '../models/reserva.model';

@Injectable({
  providedIn: 'root'
})
export class ReservasService {
  private readonly url = `${API_CONFIG.baseUrl}/reservas`;

  constructor(private readonly http: HttpClient) {}

  consultarMisReservas(grupo: GrupoReservasCliente, page: number, filtros: { codigo: string; cabana: string; desde: string; hasta: string } = { codigo: '', cabana: '', desde: '', hasta: '' }): Observable<MisReservasPaginaResponse> {
    let params = new HttpParams().set('grupo', grupo).set('page', page).set('size', 20);
    for (const clave of ['codigo', 'cabana', 'desde', 'hasta'] as const) {
      const valor = filtros[clave].trim();
      if (valor) params = params.set(clave, valor);
    }
    return this.http.get<MisReservasPaginaResponse>(`${this.url}/mis-reservas`, { params })
      .pipe(timeout({ first: 15000 }));
  }

  listarCabanasDelPanel(): Observable<{ idCabana: number; nombre: string }[]> {
    return this.http.get<{ idCabana: number; nombre: string }[]>(`${this.url}/panel/cabanas`)
      .pipe(timeout({ first: 15000 }));
  }

  buscarPorCodigo(codigo: string): Observable<ReservaResponse> {
    return this.http.get<ReservaResponse>(`${this.url}/codigo/${encodeURIComponent(codigo.trim())}`)
      .pipe(timeout({ first: 15000 }));
  }

  consultarDetalle(idReserva: number): Observable<ReservaDetalleResponse> {
    return this.http.get<ReservaDetalleResponse>(`${this.url}/panel/${idReserva}`)
      .pipe(timeout({ first: 15000 }));
  }

  registrarCheckout(idReserva: number, request: CheckoutRequest): Observable<CheckoutResponse> {
    return this.http.post<CheckoutResponse>(`${this.url}/${idReserva}/check-out`, request)
      .pipe(timeout({ first: 15000 }));
  }

  listarReservas(buscar: string, idCabana: number | null, page: number, size: number): Observable<ReservasPaginaResponse> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (buscar.trim()) params = params.set('buscar', buscar.trim());
    if (idCabana !== null) params = params.set('idCabana', idCabana);
    return this.http.get<ReservasPaginaResponse>(`${this.url}/panel`, { params })
      .pipe(timeout({ first: 15000 }));
  }

  consultarDisponibilidad(
    idCabana: number,
    fechaIngreso: string,
    fechaSalida: string,
    cantidadHuespedes: number,
    horaIngreso = "11:00", horaSalida = "13:00"
  ): Observable<CotizacionReservaResponse> {
    const params = new HttpParams()
      .set('fechaIngreso', fechaIngreso)
      .set('fechaSalida', fechaSalida)
      .set('cantidadHuespedes', cantidadHuespedes)
      .set('horaIngreso', horaIngreso).set('horaSalida', horaSalida);

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
