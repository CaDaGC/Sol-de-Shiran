import { API_CONFIG } from './api.config';


export function imagenUrl(valor: string | null | undefined): string {
  if (!valor) return '';
  const origen = new URL(API_CONFIG.baseUrl).origin;
  if (esFotoSubida(valor)) return origen + valor;
  try {
    const url = new URL(valor);
    if (esFotoSubida(url.pathname)) return origen + url.pathname;
  } catch {                                                           }
  return valor;
}

function esFotoSubida(ruta: string): boolean {
  return ruta.startsWith('/api/restaurante/imagenes/') || ruta.startsWith('/api/v1/cabanas/fotos/') || ruta.startsWith('/api/v1/usuarios/fotos/');
}
