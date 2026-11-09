import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth';

export const guardianGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.estaAutenticado()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }

  return ['ADMIN', 'DUENO', 'TRABAJADOR', 'GUARDIAN'].includes(auth.usuarioActual()?.rol ?? '')
    ? true
    : router.createUrlTree(['/catalogo']);
};
