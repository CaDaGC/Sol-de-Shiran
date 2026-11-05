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

export interface FechasOcupadasResponse {
  idCabana: number;
  desde: string;
  hasta: string;
  fechasOcupadas: string[];
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

export interface ReservaPanelResponse {
  idReserva: number;
  idUsuario: number;
  nombreUsuario: string | null;
  emailUsuario: string | null;
  idCabana: number;
  nombreCabana: string | null;
  fechaIngreso: string;
  fechaSalida: string;
  cantidadHuespedes: number;
  montoTotal: number;
  montoAdelanto: number;
  saldoPendiente: number;
  estadoReserva: string;
}

export interface ReservasPaginaResponse {
  content: ReservaPanelResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
