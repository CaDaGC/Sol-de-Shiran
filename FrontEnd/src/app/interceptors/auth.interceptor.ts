import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  const esLogin = req.method === 'POST' && req.url.includes('/api/v1/auth/login');

  const esConsultaPublica =
    req.method === 'GET' &&
    (/\/api\/v1\/cabanas(?:\/\d+)?(?:\?.*)?$/.test(req.url) ||
      /\/api\/v1\/reservas\/cabanas\/\d+\/(disponibilidad|fechas-ocupadas)(?:\?.*)?$/.test(
        req.url,
      ));

  if (esLogin || esConsultaPublica || !authService.estaAutenticado()) {
    return next(req);
  }

  const token = localStorage.getItem('token');

  if (!token) {
    return next(req);
  }

  const requestAutenticada = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });

  return next(requestAutenticada).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        authService.cerrarSesion();
      }

      return throwError(() => error);
    }),
  );
};
