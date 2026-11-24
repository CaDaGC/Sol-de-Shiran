import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { timeout } from 'rxjs';
import { API_CONFIG } from '../core/api.config';
import { OrdenCheckoutResponse, PagoResponse } from '../models/pago.model';
@Injectable({ providedIn: 'root' })
export class PagosService {
  private readonly url = `${API_CONFIG.baseUrl}/pagos`;
  constructor(private readonly http: HttpClient) {}
  consultarPagoRestante(idReserva: number) {
    return this.http.get<PagoRestanteRegistro | null>(`${this.url}/reservas/${idReserva}/restante`);
  }
  registrarPagoRestante(idReserva: number, request: PagoRestanteRequest) {
    return this.http.post<PagoRestanteRegistro>(`${this.url}/reservas/${idReserva}/restante`, request)
      .pipe(timeout({ first: 30000 }));
  }
  crearOrden(idReserva: number, payerEmail: string) {
    return this.http.post<OrdenCheckoutResponse>(`${this.url}/reservas/${idReserva}/orden`, { payerEmail })
      .pipe(timeout({ first: 30000 }));
  }
  consultarEstado(idTransaccion: number) {
    return this.http.get<PagoResponse>(`${this.url}/transacciones/${idTransaccion}`)
      .pipe(timeout({ first: 30000 }));
  }
}
export interface PagoRestanteRequest {
  metodoPago: 'EFECTIVO' | 'TARJETA' | 'YAPE_BILLETERA';
  numeroOperacion: string | null;
  claveIdempotencia: string;
}
export interface PagoRestanteRegistro extends PagoRestanteRequest {
  id: number;
  monto: number;
  completado: boolean;
}
