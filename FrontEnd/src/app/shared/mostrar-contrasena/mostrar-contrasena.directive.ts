import { afterNextRender, DestroyRef, Directive, ElementRef, inject, input, Renderer2 } from '@angular/core';

@Directive({ selector: 'input[appMostrarContrasena]', standalone: true })
export class MostrarContrasenaDirective {
  readonly etiqueta = input('contraseña', { alias: 'appMostrarContrasena' });
  private readonly elemento = inject<ElementRef<HTMLInputElement>>(ElementRef);
  private readonly renderer = inject(Renderer2);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    afterNextRender(() => {
      const campo = this.elemento.nativeElement;
      const contenedor = this.renderer.createElement('span');
      this.renderer.addClass(contenedor, 'campo-contrasena');
      this.renderer.insertBefore(this.renderer.parentNode(campo), contenedor, campo);
      this.renderer.appendChild(contenedor, campo);
      const boton = this.renderer.createElement('button') as HTMLButtonElement;
      this.renderer.setAttribute(boton, 'type', 'button');
      this.renderer.addClass(boton, 'boton-contrasena');
      this.renderer.appendChild(contenedor, boton);
      let visible = false;
      const actualizar = () => {
        this.renderer.setAttribute(campo, 'type', visible ? 'text' : 'password');
        this.renderer.setAttribute(boton, 'aria-label', (visible ? 'Ocultar ' : 'Mostrar ') + (this.etiqueta() || 'contraseña'));
        this.renderer.setAttribute(boton, 'title', (visible ? 'Ocultar ' : 'Mostrar ') + (this.etiqueta() || 'contraseña'));
        this.renderer.setAttribute(boton, 'aria-pressed', String(visible));
        this.renderer.setProperty(boton, 'innerHTML', '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/>' + (visible ? '<path d="m3 3 18 18"/>' : '') + '</svg>');
      };
      actualizar();
      this.destroyRef.onDestroy(this.renderer.listen(boton, 'click', (evento: MouseEvent) => {
        evento.preventDefault(); evento.stopPropagation();
        if (campo.disabled) return;
        visible = !visible; actualizar();
      }));
    });
  }
}
