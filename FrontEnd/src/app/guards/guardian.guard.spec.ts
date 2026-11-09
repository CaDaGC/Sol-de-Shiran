import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService, UsuarioAutenticado } from '../services/auth';
import { guardianGuard } from './guardian.guard';

describe('Acceso al panel del guardián', () => {
  const usuarioActual = signal<UsuarioAutenticado | null>(null);
  let autenticado = false;

  beforeEach(() => {
    autenticado = false;
    usuarioActual.set(null);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { usuarioActual, estaAutenticado: () => autenticado } }
      ]
    });
  });

  function acceder() {
    return TestBed.runInInjectionContext(() => guardianGuard(
      {} as ActivatedRouteSnapshot,
      { url: '/guardian/reservas' } as RouterStateSnapshot
    ));
  }

  it('envía al visitante al login y conserva el destino', () => {
    expect(acceder()).toEqual(TestBed.inject(Router).createUrlTree(['/login'], {
      queryParams: { returnUrl: '/guardian/reservas' }
    }));
  });

  it('permite ingresar al guardián autenticado', () => {
    autenticado = true;
    usuarioActual.set({ nombre: 'Antonella', email: 'guardian@example.com', rol: 'GUARDIAN' });
    expect(acceder()).toBe(true);
  });

  it('impide el acceso a cuentas con otros roles', () => {
    autenticado = true;
    for (const rol of ['CLIENTE', 'USUARIO']) {
      usuarioActual.set({ nombre: 'Usuario', email: 'usuario@example.com', rol });
      expect(acceder()).toEqual(TestBed.inject(Router).createUrlTree(['/catalogo']));
    }
  });
});
