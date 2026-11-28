export type TipoCategoriaMenu = 'COMIDA' | 'BEBIDA';
export type EstadoRestaurante = 'ACTIVO' | 'INACTIVO';
export type EstadoPedido = 'NUEVO' | 'EN_PREPARACION' | 'EN_ENTREGA' | 'FINALIZADO' | 'RECHAZADO';
export type MetodoPagoPedido = 'EFECTIVO' | 'YAPE_PLIN' | 'TARJETA_POS';

export interface Restaurante {
  idRestaurante: number;
  nombre: string;
  especialidad: string | null;
  horarioAtencion: string;
  imagenUrl: string | null;
  estado: EstadoRestaurante;
}
export interface RestauranteDatos { nombre: string; especialidad: string; }
export interface CategoriaMenu { idCategoria: number; idRestaurante: number; nombre: string; tipo: TipoCategoriaMenu; }
export interface CategoriaMenuDatos { nombre: string; tipo: TipoCategoriaMenu; }
export interface ProductoMenu {
  idProducto: number;
  idRestaurante: number;
  idCategoria: number;
  nombreCategoria: string;
  tipo: TipoCategoriaMenu;
  nombre: string;
  descripcion: string | null;
  precio: number;
  tiempoPreparacionMin: number;
  disponible: boolean;
  imagenUrl: string | null;
}
export interface ProductoMenuDatos {
  idCategoria: number | null;
  nombre: string;
  descripcion: string;
  precio: number | null;
  tiempoPreparacionMin: number | null;
  disponible: boolean;
}
export interface AsignacionRestaurante { idAsignacion: number; idRestaurante: number; idUsuario: number; }
export interface PedidoPanel {
  idPedido: number;
  codigoPedido: string;
  nombreCabana: string;
  nombreHuesped: string;
  montoTotal: number;
  estadoPedido: EstadoPedido;
  metodoPagoPresencial: MetodoPagoPedido;
  estadoPago: 'PENDIENTE' | 'PAGADO';
  fechaPedido: string;
  fechaEntrega: string | null;
  motivoRechazo: string | null;
}
export interface DetallePedidoMenu {
  idProducto: number;
  nombreProducto: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  observaciones: string | null;
}
export interface PedidoRestaurante extends PedidoPanel {
  idRestaurante: number;
  nombreRestaurante: string;
  idReserva: number;
  detalles: DetallePedidoMenu[];
}
export interface PanelPedidos {
  restaurante: Restaurante;
  resumen: { nuevos: number; enPreparacion: number; enEntrega: number; finalizadosHoy: number; };
  pedidos: PedidoPanel[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
  primera: boolean;
  ultima: boolean;
}
export const ESTADOS_PEDIDO: ReadonlyArray<{ valor: EstadoPedido; etiqueta: string }> = [
  { valor: 'NUEVO', etiqueta: 'Nuevo' },
  { valor: 'EN_PREPARACION', etiqueta: 'En preparación' },
  { valor: 'EN_ENTREGA', etiqueta: 'En entrega' },
  { valor: 'FINALIZADO', etiqueta: 'Finalizado' },
  { valor: 'RECHAZADO', etiqueta: 'Rechazado' },
];
export function etiquetaEstado(estado: EstadoPedido): string {
  return ESTADOS_PEDIDO.find(opcion => opcion.valor === estado)?.etiqueta ?? estado;
}
export function etiquetaPago(metodo: MetodoPagoPedido): string {
  return { EFECTIVO: 'Efectivo', YAPE_PLIN: 'Yape / Plin', TARJETA_POS: 'Tarjeta POS' }[metodo];
}
