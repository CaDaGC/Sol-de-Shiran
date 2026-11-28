import { imagenUrl } from './imagen-url';
import { API_CONFIG } from './api.config';

describe('Fotos compartidas entre web y móvil', () => {
  it('resuelve una foto relativa contra el gateway, no contra Angular', () => {
    expect(imagenUrl('/api/v1/cabanas/fotos/abc')).toBe(new URL(API_CONFIG.baseUrl).origin + '/api/v1/cabanas/fotos/abc');
  });
  it('resuelve imágenes del menú contra el gateway', () => {
    expect(imagenUrl('/api/restaurante/imagenes/productos/12')).toBe(new URL(API_CONFIG.baseUrl).origin + '/api/restaurante/imagenes/productos/12');
  });
  it('adapta las fotos del gateway guardadas con otra dirección', () => {
    expect(imagenUrl('https://ejemplo.ngrok-free.dev/api/v1/cabanas/fotos/abc'))
      .toBe(new URL(API_CONFIG.baseUrl).origin + '/api/v1/cabanas/fotos/abc');
    expect(imagenUrl('/images/cabana.svg')).toBe('/images/cabana.svg');
  });
});
