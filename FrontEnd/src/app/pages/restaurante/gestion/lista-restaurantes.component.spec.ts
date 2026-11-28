import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ListaRestaurantesComponent } from './lista-restaurantes.component';
import { API_CONFIG } from '../../../core/api.config';

describe('Imagen dentro del formulario de restaurante', () => {
  let http: HttpTestingController;
  beforeEach(() => { TestBed.configureTestingModule({ imports: [ListaRestaurantesComponent], providers: [provideHttpClient(), provideHttpClientTesting()] }); http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());
  it('guarda el restaurante y la imagen antes de dar por terminado el formulario', () => {
    const f = TestBed.createComponent(ListaRestaurantesComponent); f.componentRef.setInput('locales', []);
    const c = f.componentInstance; c.formulario = { nombre: 'Nuevo restaurante', especialidad: 'Regional' };
    const archivo = new File(['imagen'], 'imagen.png', { type: 'image/png' }); c.imagen = { archivo, quitar: false, valida: true };
    const cambio = vi.fn(); c.actualizado.subscribe(cambio); c.guardar();
    const crear = http.expectOne(API_CONFIG.gatewayUrl + '/restaurante/admin/locales'); expect(crear.request.method).toBe('POST');
    crear.flush({ idRestaurante: 15, nombre: 'Nuevo restaurante', imagenUrl: null });
    const foto = http.expectOne(API_CONFIG.gatewayUrl + '/restaurante/admin/locales/15/foto');
    expect(foto.request.body.get('file')).toBe(archivo); expect(c.ocupado()).toBe(true); expect(cambio).not.toHaveBeenCalled();
    foto.flush({ imagenUrl: '/api/restaurante/imagenes/locales/15' }); expect(cambio).toHaveBeenCalledOnce(); expect(c.ocupado()).toBe(false); f.destroy();
  });
  it('si falla la foto, conserva los datos y reintenta sin duplicar el restaurante', () => {
    const f = TestBed.createComponent(ListaRestaurantesComponent); f.componentRef.setInput('locales', []);
    const c = f.componentInstance; c.formulario = { nombre: 'Borrador', especialidad: '' }; c.editando.set(true);
    c.imagen = { archivo: new File(['foto'], 'foto.png', { type: 'image/png' }), quitar: false, valida: true };
    c.guardar(); http.expectOne(API_CONFIG.gatewayUrl + '/restaurante/admin/locales').flush({ idRestaurante: 16, imagenUrl: null });
    http.expectOne(API_CONFIG.gatewayUrl + '/restaurante/admin/locales/16/foto').flush({ detail: 'Almacenamiento no disponible' }, { status: 503, statusText: 'Service unavailable' });
    expect(c.idEdicion).toBe(16); expect(c.editando()).toBe(true); expect(c.formulario.nombre).toBe('Borrador');
    c.guardar(); const editar = http.expectOne(API_CONFIG.gatewayUrl + '/restaurante/admin/locales/16'); expect(editar.request.method).toBe('PUT'); editar.flush({ idRestaurante: 16, imagenUrl: null });
    http.expectOne(API_CONFIG.gatewayUrl + '/restaurante/admin/locales/16/foto').flush({ imagenUrl: '/foto' }); f.destroy();
  });
});
