import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, inicioPorRol } from '../services/auth';

export const catalogoGuard: CanActivateFn = () => {
  const rol = inject(AuthService).usuarioActual()?.rol;
  const router = inject(Router);
  return !rol || rol === 'ADMIN' || rol === 'CLIENTE'
    ? true : router.createUrlTree([inicioPorRol(rol)]);
};

export const cuentaGuard: CanActivateFn = (_route, state) => {
  return accesoAutenticado(state.url, ['ADMIN', 'CLIENTE', 'TRABAJADOR']);
};

export const pagoGuard: CanActivateFn = (_route, state) => {
  return accesoAutenticado(state.url, ['ADMIN', 'CLIENTE']);
};

function accesoAutenticado(url: string, roles: string[]) {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.estaAutenticado()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: url } });
  }
  const rol = auth.usuarioActual()?.rol;
  return roles.includes(rol ?? '')
    ? true : router.createUrlTree([inicioPorRol(rol)]);
}
