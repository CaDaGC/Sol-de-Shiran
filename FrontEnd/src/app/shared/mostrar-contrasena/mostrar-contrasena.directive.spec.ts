import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MostrarContrasenaDirective } from './mostrar-contrasena.directive';

@Component({ standalone: true, imports: [FormsModule, MostrarContrasenaDirective], template: '<form (ngSubmit)="enviado = true"><input name="clave" type="password" appMostrarContrasena="contraseña" [(ngModel)]="clave" /></form>' })
class FormularioPrueba {
  clave = 'Clave1234';
  enviado = false;
}

describe('Visibilidad de contraseñas', () => {
  it('alterna el ojo sin perder el valor ni enviar el formulario', async () => {
    TestBed.configureTestingModule({ imports: [FormularioPrueba] });
    const f = TestBed.createComponent(FormularioPrueba); f.detectChanges(); await f.whenStable();
    const input = f.nativeElement.querySelector('input') as HTMLInputElement;
    const boton = f.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(input.type).toBe('password'); expect(boton.getAttribute('aria-label')).toBe('Mostrar contraseña');
    boton.click(); expect(input.type).toBe('text'); expect(boton.getAttribute('aria-pressed')).toBe('true');
    expect(input.value).toBe('Clave1234'); expect(f.componentInstance.enviado).toBe(false);
    boton.click(); expect(input.type).toBe('password'); expect(f.componentInstance.clave).toBe('Clave1234'); f.destroy();
  });
});
