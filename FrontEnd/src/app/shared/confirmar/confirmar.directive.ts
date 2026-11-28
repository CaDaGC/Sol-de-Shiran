import { FormularioDirective } from '../formulario/formulario.directive';
import { afterNextRender, DestroyRef, Directive, ElementRef, inject, input } from '@angular/core';

@Directive({ selector: '[appConfirmar]', standalone: true })
export class ConfirmarDirective {
  readonly appConfirmar = input.required<string>();
  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly destroyRef = inject(DestroyRef);
  private readonly formulario = inject(FormularioDirective, { optional: true, self: true });
  private autorizado = false;
  private dialogo?: HTMLDialogElement;

  constructor() {
    afterNextRender(() => {
      const tipo = this.elemento instanceof HTMLFormElement ? 'submit' : 'click';
      const confirmar = (evento: Event) => {
        if (this.autorizado) { this.autorizado = false; return; }
        if (evento.defaultPrevented) return;
        if (this.formulario && !this.formulario.validar()) {
          evento.preventDefault(); evento.stopImmediatePropagation(); return;
        }
        evento.preventDefault();
        evento.stopImmediatePropagation();
        if (this.dialogo?.open) return;
        const origen = evento instanceof SubmitEvent ? evento.submitter : null;
        const dialogo = document.createElement('dialog');
        this.dialogo = dialogo;
        dialogo.className = 'confirmacion-escritura';
        const titulo = document.createElement('h2');
        titulo.id = 'confirmar-' + crypto.randomUUID();
        titulo.textContent = 'Confirmar operación';
        dialogo.setAttribute('aria-labelledby', titulo.id);
        const texto = document.createElement('p');
        texto.textContent = this.appConfirmar();
        const acciones = document.createElement('div');
        acciones.className = 'confirmacion-escritura-acciones';
        const cancelar = document.createElement('button');
        cancelar.type = 'button'; cancelar.className = 'btn btn-outline-secondary'; cancelar.textContent = 'Cancelar';
        const aceptar = document.createElement('button');
        aceptar.type = 'button'; aceptar.className = 'btn btn-success'; aceptar.textContent = 'Confirmar';
        const cerrar = () => { dialogo.close(); dialogo.remove(); this.dialogo = undefined; };
        cancelar.onclick = cerrar;
        dialogo.oncancel = evento => { evento.preventDefault(); };
        aceptar.onclick = () => {
          cerrar();
          if (!this.elemento.isConnected) return;
          this.autorizado = true;
          if (this.elemento instanceof HTMLFormElement) this.elemento.requestSubmit(origen instanceof HTMLButtonElement ? origen : undefined);
          else this.elemento.click();
          this.autorizado = false;
        };
        acciones.append(cancelar, aceptar); dialogo.append(titulo, texto, acciones);
        document.body.append(dialogo); dialogo.showModal(); cancelar.focus();
      };
      this.elemento.addEventListener(tipo, confirmar, true);
      this.destroyRef.onDestroy(() => { this.elemento.removeEventListener(tipo, confirmar, true); this.dialogo?.remove(); });
    });
  }
}
