import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_CONFIG } from '../core/api.config';
import { AsignacionRestaurante, CategoriaMenu, CategoriaMenuDatos, EstadoRestaurante, ProductoMenu, ProductoMenuDatos, Restaurante, RestauranteDatos } from '../models/restaurante.model';

@Injectable({ providedIn: 'root' })
export class RestauranteService {
  private readonly http = inject(HttpClient);
  private readonly base = API_CONFIG.gatewayUrl + '/restaurante';
  listar() { return this.http.get<Restaurante[]>(this.base + '/admin/locales'); }
  miLocal(idUsuario: number) { return this.http.get<Restaurante>(this.base + '/mi-local/' + idUsuario); }
  crear(datos: RestauranteDatos) { return this.http.post<Restaurante>(this.base + '/admin/locales', datos); }
  editar(id: number, datos: RestauranteDatos) { return this.http.put<Restaurante>(this.base + '/admin/locales/' + id, datos); }
  cambiarEstado(id: number, estado: EstadoRestaurante) { return this.http.patch<Restaurante>(this.base + '/admin/locales/' + id + '/estado', { estado }); }
  categorias(id: number) { return this.http.get<CategoriaMenu[]>(this.base + '/admin/locales/' + id + '/categorias'); }
  crearCategoria(id: number, datos: CategoriaMenuDatos) { return this.http.post<CategoriaMenu>(this.base + '/admin/locales/' + id + '/categorias', datos); }
  editarCategoria(id: number, categoria: number, datos: CategoriaMenuDatos) { return this.http.put<CategoriaMenu>(this.base + '/admin/locales/' + id + '/categorias/' + categoria, datos); }
  eliminarCategoria(id: number, categoria: number) { return this.http.delete<void>(this.base + '/admin/locales/' + id + '/categorias/' + categoria); }
  productos(id: number) { return this.http.get<ProductoMenu[]>(this.base + '/admin/locales/' + id + '/productos'); }
  cartaOperativa(id: number) { return this.http.get<ProductoMenu[]>(this.base + '/locales/' + id + '/carta-operativa'); }
  crearProducto(id: number, datos: ProductoMenuDatos) { return this.http.post<ProductoMenu>(this.base + '/admin/locales/' + id + '/productos', datos); }
  editarProducto(id: number, producto: number, datos: ProductoMenuDatos) { return this.http.put<ProductoMenu>(this.base + '/admin/locales/' + id + '/productos/' + producto, datos); }
  disponibilidad(id: number, producto: number, disponible: boolean) { return this.http.patch<ProductoMenu>(this.base + '/locales/' + id + '/productos/' + producto + '/disponibilidad', { disponible }); }
  asignaciones(id: number) { return this.http.get<AsignacionRestaurante[]>(this.base + '/admin/locales/' + id + '/asignaciones'); }
  asignar(id: number, idUsuario: number) { return this.http.put<AsignacionRestaurante>(this.base + '/admin/locales/' + id + '/asignaciones', { idUsuario }); }
  retirarAsignacion(id: number, usuario: number) { return this.http.delete<void>(this.base + '/admin/locales/' + id + '/asignaciones/' + usuario); }
  subirFoto(tipo: 'locales' | 'productos', id: number, archivo: File) {
    const datos = new FormData(); datos.append('file', archivo);
    return this.http.post<{ imagenUrl: string }>(this.base + '/admin/' + tipo + '/' + id + '/foto', datos);
  }
  quitarFoto(tipo: 'locales' | 'productos', id: number) { return this.http.delete<void>(this.base + '/admin/' + tipo + '/' + id + '/foto'); }
}
