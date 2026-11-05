import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../services/auth';
import { BusinessLayoutComponent } from './business-layout.component';

describe('Menú lateral contraíble', () => {
  let fixture: ComponentFixture<BusinessLayoutComponent>;

  beforeEach(() => {
    localStorage.removeItem('sidebar-contraido');
    TestBed.configureTestingModule({
      imports: [BusinessLayoutComponent],
      providers: [provideRouter([]), {
        provide: AuthService,
        useValue: { usuarioActual: signal({ nombre: 'Antonella', email: 'guardian@example.com', rol: 'GUARDIAN' }) }
      }]
    });
  });

  afterEach(() => {
    fixture?.destroy();
    localStorage.removeItem('sidebar-contraido');
  });

  function crear() {
    fixture = TestBed.createComponent(BusinessLayoutComponent);
    fixture.detectChanges();
    fixture.detectChanges();
  }

  it('contrae y expande el menú, ajustando el layout y guardando la preferencia', () => {
    crear();
    const boton: HTMLButtonElement = fixture.nativeElement.querySelector('.sidebar-toggle');
    expect(boton.getAttribute('aria-expanded')).toBe('true');
    boton.click();
    fixture.detectChanges();
    expect(boton.getAttribute('aria-label')).toBe('Expandir menú');
    expect(fixture.nativeElement.querySelector('.business-layout').classList.contains('sidebar-collapsed')).toBe(true);
    expect(localStorage.getItem('sidebar-contraido')).toBe('true');
    expect(fixture.nativeElement.querySelector('a[aria-label="Reservas"]').getAttribute('title')).toBe('Reservas');
    boton.click();
    fixture.detectChanges();
    expect(boton.getAttribute('aria-expanded')).toBe('true');
    expect(localStorage.getItem('sidebar-contraido')).toBe('false');
  });

  it('restaura el estado guardado al volver a abrir una vista con sidebar', () => {
    localStorage.setItem('sidebar-contraido', 'true');
    crear();
    expect(fixture.componentInstance.contraido()).toBe(true);
    expect(fixture.nativeElement.querySelector('.sidebar-toggle').getAttribute('aria-expanded')).toBe('false');
  });
});
