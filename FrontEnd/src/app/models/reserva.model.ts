export interface CotizacionReservaResponse {
  idCabana: number;
  nombreCabana: string;
  fechaIngreso: string;
  fechaSalida: string;
  cantidadHuespedes: number;
  noches: number;
  tarifaNoche: number;
  montoTotal: number;
  montoAdelanto: number;
  saldoPendiente: number;
  disponible: boolean;
  mensaje: string;
}

export interface CrearReservaRequest {
  idCabana: number;
  fechaIngreso: string;
  fechaSalida: string;
  cantidadHuespedes: number;
}

export interface ReservaResponse {
  idReserva: number;
  idCabana: number;
  idUsuario: number;
  fechaIngreso: string;
  fechaSalida: string;
  cantidadHuespedes: number;
  tarifaNocheAplicada: number;
  montoTotal: number;
  montoAdelanto: number;
  saldoPendiente: number;
  estadoReserva: string;
  expiraEn: string;
}
