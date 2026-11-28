import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { inject, Injectable, PLATFORM_ID } from '@angular/core';
import { EMPTY, filter, fromEvent, merge, timer } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ActualizacionAutomaticaService {
  private readonly documento = inject(DOCUMENT);
  private readonly plataforma = inject(PLATFORM_ID);
  observar(duranteModal = false) {
    const ventana = this.documento.defaultView;
    if (!isPlatformBrowser(this.plataforma) || !ventana) return EMPTY;
    return merge(timer(5000, 5000), fromEvent(ventana, 'focus')).pipe(
      filter(() => !this.documento.hidden && (duranteModal || !this.documento.querySelector('dialog[open], [aria-modal="true"]'))),
    );
  }
}
