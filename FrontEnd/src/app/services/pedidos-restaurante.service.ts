import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_CONFIG } from '../core/api.config';
import { EstadoPedido, PanelPedidos, PedidoRestaurante } from '../models/restaurante.model';

@Injectable({ providedIn: 'root' })
export class PedidosRestauranteService {
  private readonly http = inject(HttpClient);
  private readonly base = API_CONFIG.gatewayUrl + '/pedidos/restaurante';
  panel(id: number, estado: EstadoPedido | '', cabana: string, pagina: number) {
    let params = new HttpParams().set('pagina', pagina).set('tamanio', 20);
    if (estado) params = params.set('estado', estado);
    if (cabana.trim()) params = params.set('cabana', cabana.trim());
    return this.http.get<PanelPedidos>(this.base + '/' + id, { params });
  }
  detalle(local: number, pedido: number) { return this.http.get<PedidoRestaurante>(this.base + '/' + local + '/' + pedido); }
  cambiarEstado(local: number, pedido: number, estado: EstadoPedido, motivo?: string) {
    return this.http.patch<PedidoRestaurante>(this.base + '/' + local + '/' + pedido + '/estado', { estado, ...(motivo ? { motivo: motivo.trim() } : {}) });
  }
  comanda(local: number, pedido: number) { return this.http.get(this.base + '/' + local + '/' + pedido + '/comanda', { responseType: 'blob' }); }
}
