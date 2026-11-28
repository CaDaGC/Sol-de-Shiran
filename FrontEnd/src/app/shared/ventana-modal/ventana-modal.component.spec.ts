import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { VentanaModalComponent } from './ventana-modal.component';

@Component({ standalone: true, imports: [FormsModule, VentanaModalComponent], template: '<app-ventana-modal titulo="Formulario" (cerrar)="cerrada = true"><input name="dato" [(ngModel)]="dato" /></app-ventana-modal>' })
class FormularioPrueba { dato = 'Borrador sin guardar'; cerrada = false; }

describe('Protección de borradores en modales', () => {
  const original = Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, 'showModal');
  beforeAll(() => Object.defineProperty(HTMLDialogElement.prototype, 'showModal', { configurable: true, value(this: HTMLDialogElement) { this.open = true; } }));
  afterAll(() => { if (original) Object.defineProperty(HTMLDialogElement.prototype, 'showModal', original); else Reflect.deleteProperty(HTMLDialogElement.prototype, 'showModal'); });
  it('clic exterior y Escape conservan el formulario, pero la X permite cerrarlo', async () => {
    const f = TestBed.createComponent(FormularioPrueba); f.detectChanges(); await f.whenStable();
    const dialogo = f.nativeElement.querySelector('dialog') as HTMLDialogElement;
    dialogo.click(); const escape = new Event('cancel', { cancelable: true }); dialogo.dispatchEvent(escape);
    expect(escape.defaultPrevented).toBe(true); expect(f.componentInstance.cerrada).toBe(false);
    expect(f.componentInstance.dato).toBe('Borrador sin guardar'); expect(dialogo.open).toBe(true);
    (f.nativeElement.querySelector('.btn-close') as HTMLButtonElement).click(); expect(f.componentInstance.cerrada).toBe(true); f.destroy();
  });
});
