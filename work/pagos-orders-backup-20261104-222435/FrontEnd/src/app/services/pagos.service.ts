import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { API_CONFIG } from '../core/api.config';
import {
  CrearPagoBrickRequest,
  PagoResponse
} from '../models/pago.model';

@Injectable({
  providedIn: 'root'
})
export class PagosService {
  private readonly url = `${API_CONFIG.baseUrl}/pagos`;

  constructor(private readonly http: HttpClient) {}

  procesarPagoBrick(
    idReserva: number,
    request: CrearPagoBrickRequest
  ): Observable<PagoResponse> {
    return this.http
      .post<PagoResponse>(
        `${this.url}/reservas/${idReserva}/brick`,
        request
      )
      .pipe(timeout({ first: 30000 }));
  }
}
