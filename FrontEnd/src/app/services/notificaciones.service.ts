import { Injectable, OnDestroy, signal } from '@angular/core';

export interface Notificacion {
  tipo: 'exito' | 'error';
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class NotificacionesService implements OnDestroy {
  readonly aviso = signal<Notificacion | null>(null);
  private temporizador: ReturnType<typeof setTimeout> | undefined;

  mostrar(tipo: Notificacion['tipo'], mensaje: string): void {
    this.cerrar();
    this.aviso.set({ tipo, mensaje });
    this.temporizador = setTimeout(() => this.cerrar(), 5_000);
  }

  cerrar(): void {
    if (this.temporizador) clearTimeout(this.temporizador);
    this.temporizador = undefined;
    this.aviso.set(null);
  }

  ngOnDestroy(): void { this.cerrar(); }
}
