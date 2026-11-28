import { HttpErrorResponse } from '@angular/common/http';

export function mensajeErrorApi(error: unknown, alternativa: string): string {
  if (!(error instanceof HttpErrorResponse)) return alternativa;
  if (error.status === 0) return 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo nuevamente.';
  if (error.status === 401) return 'Tu sesión expiró. Inicia sesión nuevamente.';
  const detalle = error.error;
  if (detalle && typeof detalle === 'object' && !(detalle instanceof Blob)) {
    const mensaje = typeof detalle.detail === 'string' ? detalle.detail : '';
    const errores = Array.isArray(detalle.errores) ? detalle.errores.filter((valor: unknown) => typeof valor === 'string') : [];
    if (errores.length) return [mensaje, ...errores].filter(Boolean).join(' ');
    if (mensaje) return mensaje;
  }
  if (error.status === 403) return 'No tienes permiso para realizar esta operación.';
  return alternativa;
}
