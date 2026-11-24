import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, inicioPorRol } from '../services/auth';

export const clienteGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.estaAutenticado()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  return auth.usuarioActual()?.rol === 'CLIENTE' ? true : router.createUrlTree([inicioPorRol(auth.usuarioActual()?.rol)]);
};
