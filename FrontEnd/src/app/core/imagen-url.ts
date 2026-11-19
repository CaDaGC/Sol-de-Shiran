import { API_CONFIG } from './api.config';

// Las fotos subidas pertenecen al gateway, no al servidor de Angular.
export function imagenUrl(valor: string | null | undefined): string {
  if (!valor) return '';
  const origen = new URL(API_CONFIG.baseUrl).origin;
  if (valor.startsWith('/api/v1/cabanas/fotos/')) return origen + valor;
  try {
    const url = new URL(valor);
    if (url.pathname.startsWith('/api/v1/cabanas/fotos/')) return origen + url.pathname;
  } catch { /* Las rutas /images y /assets pertenecen al frontend. */ }
  return valor;
}
