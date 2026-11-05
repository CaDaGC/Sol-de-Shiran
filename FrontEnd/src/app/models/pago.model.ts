export interface CrearPagoBrickRequest {
  token: string;
  paymentMethodId: string;
  installments: number;
  issuerId: string | null;
  payerEmail: string;
  identificationType: string;
  identificationNumber: string;
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

export interface OrdenCheckoutResponse {
  idTransaccion: number;
  orderId: string;
  checkoutUrl: string;
  monto: number;
  moneda: string;
}
