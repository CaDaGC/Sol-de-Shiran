import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth';
import { PublicNavbarComponent } from './public-navbar.component';

describe('Navegación por secciones del inicio', () => {
  const router = { url: '/catalogo' };
  let destino: HTMLElement;
  let componente: PublicNavbarComponent;
  const desplazar = vi.fn();
  beforeEach(() => {
    router.url = '/catalogo';
    desplazar.mockClear();
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: { usuarioActual: signal(null) } },
      ],
    });
    componente = TestBed.runInInjectionContext(() => new PublicNavbarComponent());
    destino = document.createElement('section');
    destino.id = 'naturaleza';
    destino.scrollIntoView = desplazar;
    document.body.append(destino);
  });
  afterEach(() => destino.remove());
  it('baja a Naturaleza sin salir del inicio', () => {
    const evento = new MouseEvent('click', { cancelable: true });
    componente.irASeccion(evento, 'naturaleza');
    expect(evento.defaultPrevented).toBe(true);
    expect(desplazar).toHaveBeenCalledWith({ block: 'start' });
  });
  it('desde otra pantalla permite que RouterLink abra el inicio con su fragmento', () => {
    router.url = '/mis-reservas';
    const evento = new MouseEvent('click', { cancelable: true });
    componente.irASeccion(evento, 'naturaleza');
    expect(evento.defaultPrevented).toBe(false);
    expect(desplazar).not.toHaveBeenCalled();
  });
});
