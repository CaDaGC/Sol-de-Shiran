import { afterEveryRender, afterNextRender, DestroyRef, Directive, ElementRef, inject } from '@angular/core';

@Directive({ selector: 'form', standalone: true })
export class FormularioDirective {
  private readonly formulario = inject<ElementRef<HTMLFormElement>>(ElementRef).nativeElement;
  private readonly destroyRef = inject(DestroyRef);
  private enviado = false;
  private readonly visitados = new WeakSet<Element>();
  private readonly mensajes = new Map<HTMLElement, HTMLElement>();

  constructor() {
    afterEveryRender(() => this.actualizar());
    afterNextRender(() => {
      this.formulario.noValidate = true;
      const salir = (evento: FocusEvent) => {
        if (evento.target instanceof HTMLElement) this.visitados.add(evento.target);
        this.actualizar();
      };
      const enviar = (evento: Event) => {
        if (!this.validar()) {
          evento.preventDefault();
          evento.stopImmediatePropagation();

        }
      };
      this.formulario.addEventListener('focusout', salir);
      this.formulario.addEventListener('submit', enviar, true);
      this.destroyRef.onDestroy(() => {
        this.formulario.removeEventListener('focusout', salir);
        this.formulario.removeEventListener('submit', enviar, true);
      });
    });
  }

  validar(): boolean {
    this.enviado = true;
    this.actualizar();
    const invalido = this.formulario.querySelector<HTMLElement>('[aria-invalid="true"]');
    invalido?.focus();
    return !invalido;
  }

  private actualizar(): void {
    this.formulario.querySelectorAll<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>('input, select, textarea').forEach(campo => {
      const obligatorio = campo.required && !('readOnly' in campo && campo.readOnly);
      const etiqueta = campo.labels?.item(0);
      if (etiqueta && campo.type !== 'radio' && campo.type !== 'checkbox') {
        let marca = etiqueta.querySelector<HTMLElement>('.campo-obligatorio');
        if (obligatorio && !marca) {
          marca = document.createElement('span');
          marca.className = 'campo-obligatorio';
          marca.textContent = ' *';
          marca.setAttribute('aria-hidden', 'true');
          etiqueta.insertBefore(marca, etiqueta.querySelector('input, select, textarea, .campo-contrasena'));
        }
        if (marca) marca.hidden = !obligatorio;
      }
      const vacio = !campo.value.trim() || (campo.type === 'radio' && !this.formulario.querySelector('input[type="radio"][name="' + campo.name + '"]:checked'));
      const invalido = !campo.matches(':disabled') && !('readOnly' in campo && campo.readOnly) && (this.enviado || this.visitados.has(campo))
        && ((obligatorio && vacio) || campo.classList.contains('ng-invalid') || !campo.validity.valid);
      campo.classList.toggle('campo-invalido', invalido);
      campo.setAttribute('aria-invalid', String(invalido));
      let mensaje = this.mensajes.get(campo);
      if (invalido && !mensaje) {
        mensaje = document.createElement('small');
        mensaje.className = 'error-campo-obligatorio';
        mensaje.id = 'error-campo-' + crypto.randomUUID();
        const contenedor = campo.closest('.campo-contrasena') ?? campo;
        contenedor.insertAdjacentElement('afterend', mensaje);
        campo.setAttribute('aria-describedby', [campo.getAttribute('aria-describedby'), mensaje.id].filter(Boolean).join(' '));
        this.mensajes.set(campo, mensaje);
      }
      if (mensaje) {
        mensaje.hidden = !invalido;
        mensaje.textContent = obligatorio && vacio ? 'Completa este campo obligatorio.' : 'Revisa el valor ingresado.';
      }
    });
  }
}
