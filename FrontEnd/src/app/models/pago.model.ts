export interface CrearPagoBrickRequest {
  token: string | null;
  paymentMethodId: string;
  installments: number;
  issuerId: string | null;
  payerEmail: string;
}

export interface PagoResponse {
  idTransaccion: number;
  idReserva: number;
  estado: 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | string;
  monto: number;
  moneda: string;
  idExternoPasarela: string | null;
  mensaje: string;
}
