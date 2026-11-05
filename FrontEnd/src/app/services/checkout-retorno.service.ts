import { Injectable } from '@angular/core';
import { ReservaResponse, CotizacionReservaResponse } from '../models/reserva.model';
export interface CheckoutRetorno {
  transaccion: number; reserva: ReservaResponse; nombreCabana: string; url: string;
  cotizacion?: CotizacionReservaResponse | null;
  adultos: number; ninos: number; imagen: number; scroll: number; fecha: number;
}
@Injectable({ providedIn: 'root' })
export class CheckoutRetornoService {
  guardar(contexto: CheckoutRetorno): void {
    try { sessionStorage.setItem('checkout-retorno-' + contexto.transaccion, JSON.stringify(contexto)); } catch { /* El retorno sin contexto sigue disponible. */ }
  }
  obtener(id: number): CheckoutRetorno | null {
    try {
      const c: CheckoutRetorno = JSON.parse(sessionStorage.getItem('checkout-retorno-' + id) ?? 'null');
      if (!c || c.transaccion !== id || !c.reserva || !Number.isSafeInteger(c.reserva.idReserva)
        || !c.url.startsWith('/catalogo/' + c.reserva.idCabana)
        || !/^\/catalogo\/\d+(?:\?.*)?$/.test(c.url)
        || Date.now() - c.fecha > 86400000) return null;
      return c;
    } catch { return null; }
  }
}
