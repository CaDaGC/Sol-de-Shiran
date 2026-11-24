import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { FormularioDirective } from './formulario.directive';
import { ConfirmarDirective } from '../confirmar/confirmar.directive';

@Component({ imports: [FormsModule, FormularioDirective, ConfirmarDirective], template: `<form appConfirmar="¿Confirmas el registro del pago?" (ngSubmit)="envios = envios + 1"><label>Referencia<input name="referencia" [(ngModel)]="referencia" required></label><label>Nota opcional<input name="nota" [(ngModel)]="nota"></label><button>Guardar</button></form>` })
class FormularioPrueba { referencia = ''; nota = ''; envios = 0; }

describe('Validación y confirmación antes de escribir', () => {
  beforeAll(() => {
    Object.defineProperty(HTMLDialogElement.prototype, 'showModal', { configurable: true, value(this: HTMLDialogElement) { this.open = true; } });
    Object.defineProperty(HTMLDialogElement.prototype, 'close', { configurable: true, value(this: HTMLDialogElement) { this.open = false; } });
  });
  it('marca solo los obligatorios al salir y bloquea el envío vacío', async () => {
    const fixture = TestBed.createComponent(FormularioPrueba);
    fixture.detectChanges(); await fixture.whenStable();
    const input = fixture.nativeElement.querySelector('input');
    input.dispatchEvent(new FocusEvent('focusout', { bubbles: true }));
    expect(input.getAttribute('aria-invalid')).toBe('true');
    expect(fixture.nativeElement.querySelectorAll('.campo-obligatorio')).toHaveLength(1);
    fixture.nativeElement.querySelector('button').click();
    expect(fixture.componentInstance.envios).toBe(0);
    expect(document.querySelector('.confirmacion-escritura')).toBeNull();
    expect(fixture.nativeElement.querySelector('[name="nota"]').getAttribute('aria-invalid')).toBe('false');
    fixture.destroy();
  });
  it('cancelar no envía y confirmar envía exactamente una vez', async () => {
    const fixture = TestBed.createComponent(FormularioPrueba);
    fixture.componentInstance.referencia = 'OPERACION123';
    fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges();
    const boton = fixture.nativeElement.querySelector('button');
    boton.click();
    expect(fixture.componentInstance.envios).toBe(0);
    document.querySelector<HTMLButtonElement>('.confirmacion-escritura button')!.click();
    expect(fixture.componentInstance.envios).toBe(0);
    boton.click();
    document.querySelector<HTMLButtonElement>('.confirmacion-escritura .btn-success')!.click();
    expect(fixture.componentInstance.envios).toBe(1);
    expect(document.querySelector('.confirmacion-escritura')).toBeNull();
    fixture.destroy();
  });
});
