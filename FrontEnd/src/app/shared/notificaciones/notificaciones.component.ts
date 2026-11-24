import { Component, effect, ElementRef, inject, viewChild } from '@angular/core';
import { NotificacionesService } from '../../services/notificaciones.service';

@Component({
  selector: 'app-notificaciones',
  standalone: true,
  templateUrl: './notificaciones.component.html',
  styleUrl: './notificaciones.component.css',
})
export class NotificacionesComponent {
  readonly notificaciones = inject(NotificacionesService);
  private readonly panel = viewChild<ElementRef<HTMLElement>>('panel');

  constructor() {
    effect(() => {
      const aviso = this.notificaciones.aviso();
      const panel = this.panel()?.nativeElement;
      if (!panel || typeof panel.showPopover !== 'function') return;
      const abierto = panel.matches(':popover-open');
      if (aviso && !abierto) panel.showPopover();
      if (!aviso && abierto) panel.hidePopover();
    });
  }
}
