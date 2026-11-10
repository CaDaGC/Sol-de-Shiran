import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth';
import { clienteGuard } from './cliente.guard';

describe('Acceso a Mis reservas', () => {
  function comprobar(autenticado: boolean, rol: string) {
    TestBed.configureTestingModule({ providers: [provideRouter([]), {
      provide: AuthService, useValue: { estaAutenticado: () => autenticado, usuarioActual: signal({ rol }) }
    }] });
    return TestBed.runInInjectionContext(() => clienteGuard({} as ActivatedRouteSnapshot,
      { url: '/mis-reservas' } as RouterStateSnapshot));
  }

  it('solicita iniciar sesión y conserva el destino', () => {
    expect(comprobar(false, '')).toEqual(TestBed.inject(Router).createUrlTree(['/login'], {
      queryParams: { returnUrl: '/mis-reservas' }
    }));
  });
  it('permite a clientes autenticados', () => { expect(comprobar(true, 'CLIENTE')).toBe(true); });
  it('no permite al guardián acceder a la pantalla del cliente', () => {
    expect(comprobar(true, 'TRABAJADOR')).toEqual(TestBed.inject(Router).createUrlTree(['/guardian/reservas']));
  });
  it('reserva el historial para clientes y devuelve al administrador al catálogo', () => {
    expect(comprobar(true, 'ADMIN')).toEqual(TestBed.inject(Router).createUrlTree(['/catalogo']));
  });
});
