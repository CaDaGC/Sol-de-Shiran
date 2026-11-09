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

export type GrupoReservasCliente = 'TODAS' | 'PENDIENTES' | 'CONFIRMADAS' | 'PASADAS';

export interface MiReservaResponse extends ReservaResponse {
  nombreCabana: string | null;
  imagenPrincipal: string | null;
  grupo: GrupoReservasCliente;
  pagoDisponible: boolean;
}

export interface MisReservasPaginaResponse {
  content: MiReservaResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
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

export interface ReservaDetalleResponse extends ReservaPanelResponse {
  tarifaNocheAplicada: number;
  expiraEn: string;
  fechaCreacion: string | null;
}

export const ESTADOS_RESERVA: Record<string, string> = {
  PENDIENTE_PAGO: 'Pendiente de pago',
  CONFIRMADA: 'Confirmada',
  OCUPADA: 'En estadía',
  FINALIZADA: 'Finalizada',
  CANCELADA: 'Cancelada',
  EXPIRADA: 'Expirada'
};
