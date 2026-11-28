import { afterNextRender, Component, ElementRef, input, output, viewChild } from '@angular/core';

let secuenciaVentanas = 0;

@Component({
  selector: 'app-ventana-modal',
  standalone: true,
  template: `<dialog #dialogo class="ventana-restaurante" [class.amplia]="amplia()" (cancel)="cancelar($event)" [attr.aria-labelledby]="idTitulo">
    <div class="modal-header gap-3"><h2 [id]="idTitulo" class="modal-title fs-4">{{ titulo() }}</h2><button type="button" class="btn-close" aria-label="Cerrar ventana" [disabled]="ocupado()" (click)="cerrar.emit()"></button></div>
    <div class="modal-body"><ng-content /></div>
  </dialog>`,
  styles: [`.ventana-restaurante{border:1px solid var(--color-line);border-radius:18px;padding:0;width:min(680px,calc(100vw - 28px));max-height:90dvh;color:inherit;background:var(--color-cream)}.ventana-restaurante.amplia{width:min(1050px,calc(100vw - 28px))}.ventana-restaurante::backdrop{background:rgb(13 30 22 / .55)}.modal-header{padding:20px 24px;border-bottom:1px solid var(--color-line);background:#fff;position:sticky;top:0;z-index:2}.modal-body{padding:24px}.btn-close{flex-shrink:0}`],
})
export class VentanaModalComponent {
  readonly idTitulo = 'titulo-ventana-' + ++secuenciaVentanas;
  readonly titulo = input.required<string>();
  readonly amplia = input(false);
  readonly ocupado = input(false);
  readonly cerrar = output<void>();
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  constructor() { afterNextRender(() => this.dialogo().nativeElement.showModal()); }
  cancelar(evento: Event): void { evento.preventDefault(); }
}
