import { Component, input } from '@angular/core';

@Component({ selector: 'app-icono', standalone: true,
  template: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="trazos[nombre()]" /></svg>`,
})
export class IconoComponent {
  readonly nombre = input<'editar' | 'ver' | 'basura' | 'activar' | 'imagen' | 'nuevo' | 'recargar'>('ver');
  readonly trazos = {
    editar: 'M16 3l5 5L9 20l-6 1 1-6L16 3zM14 5l5 5',
    ver: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12zM15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
    basura: 'M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7',
    activar: 'M4 12l5 5L20 6',
    imagen: 'M3 3h18v18H3zM3 16l6-6 5 5 3-3 4 4M16 7h.01',
    nuevo: 'M12 4v16M4 12h16',
    recargar: 'M20 7v5h-5M20 12a8 8 0 1 0-2 6M20 7l-2-2',
  };
}
