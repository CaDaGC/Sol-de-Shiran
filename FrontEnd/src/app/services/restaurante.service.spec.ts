import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RestauranteService } from './restaurante.service';
import { PedidosRestauranteService } from './pedidos-restaurante.service';
import { API_CONFIG } from '../core/api.config';
import { inicioPorRol, normalizarRol } from './auth';

describe('Contratos del módulo de restaurantes', () => {
  let http: HttpTestingController;
  beforeEach(() => { TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }); http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());
  it('consulta el panel paginado sin enviar TODOS como estado', () => {
    TestBed.inject(PedidosRestauranteService).panel(3, '', 'Luna', 2).subscribe();
    const req = http.expectOne(r => r.url === API_CONFIG.gatewayUrl + '/pedidos/restaurante/3');
    expect(req.request.params.has('estado')).toBe(false);
    expect(req.request.params.get('cabana')).toBe('Luna');
    expect(req.request.params.get('pagina')).toBe('2'); req.flush({});
  });
  it('envía rechazo con motivo sin modificar pagos o importes', () => {
    TestBed.inject(PedidosRestauranteService).cambiarEstado(3, 8, 'RECHAZADO', ' Sin ingredientes ').subscribe();
    const req = http.expectOne(API_CONFIG.gatewayUrl + '/pedidos/restaurante/3/8/estado');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ estado: 'RECHAZADO', motivo: 'Sin ingredientes' }); req.flush({});
  });
  it('sube imágenes como FormData y recupera comanda como blob', () => {
    const archivo = new File(['contenido'], 'foto.png', { type: 'image/png' });
    TestBed.inject(RestauranteService).subirFoto('productos', 9, archivo).subscribe();
    const foto = http.expectOne(API_CONFIG.gatewayUrl + '/restaurante/admin/productos/9/foto');
    expect(foto.request.body).toBeInstanceOf(FormData);
    expect(foto.request.body.get('file')).toBe(archivo); foto.flush({ imagenUrl: '/foto' });
    TestBed.inject(PedidosRestauranteService).comanda(3, 8).subscribe();
    const pdf = http.expectOne(API_CONFIG.gatewayUrl + '/pedidos/restaurante/3/8/comanda');
    expect(pdf.request.responseType).toBe('blob'); pdf.flush(new Blob(['%PDF'], { type: 'application/pdf' }));
  });
  it('dirige RESTAURANTE al panel operativo', () => {
    expect(normalizarRol('ROLE_RESTAURANTE')).toBe('RESTAURANTE');
    expect(inicioPorRol('RESTAURANTE')).toBe('/restaurante/pedidos');
  });
});
