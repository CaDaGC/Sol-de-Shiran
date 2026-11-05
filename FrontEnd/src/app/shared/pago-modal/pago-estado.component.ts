import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
@Component({
  selector: 'app-pago-estado', standalone: true, imports: [CommonModule],
  template: `<div class="payment-state" role="status" aria-live="polite">
    @if (estado === 'APROBADA') {
      <svg class="success-mark" viewBox="0 0 80 80" aria-hidden="true"><circle cx="40" cy="40" r="36"/><path d="M23 40l12 12 23-25"/></svg>
      <p class="eyebrow">ADELANTO RECIBIDO</p><h2>¡Tu reserva está confirmada!</h2>
      <p>El pago se verificó correctamente. Ya puedes preparar tu visita a Sol de Shirán.</p>
    } @else if (estado === 'RECHAZADA') {
      <div class="state-icon" aria-hidden="true">!</div><h2>El pago no se completó</h2>
      <p>Revisa el resultado en Mercado Pago antes de iniciar otro intento.</p>
    } @else {
      <div class="spinner" aria-hidden="true"></div><h2>{{ estado === 'ERROR' ? 'No pudimos verificar el pago' : 'Estamos verificando tu pago' }}</h2>
      <p>{{ mensaje || 'Espera unos momentos. Confirmaremos tu reserva cuando Mercado Pago acredite el adelanto.' }}</p>
    }
    @if (monto !== null) { <strong class="paid-amount">{{ monto | currency:'PEN':'S/ ':'1.2-2' }}</strong> }
    @if (idReserva) { <p class="reference">Reserva #{{ idReserva }} · Operación #{{ transaccion }}</p> }
  </div>`,
  styles: [`:host{display:block}.payment-state{text-align:center;padding:30px 16px;font-family:Arial,sans-serif;color:#263d2e}.payment-state h2{font:500 1.8rem Georgia,serif;margin:12px 0}.payment-state p{line-height:1.65;color:#647067;max-width:450px;margin:12px auto}.eyebrow{font-size:.7rem;letter-spacing:.14em;font-weight:700;color:#337245!important}.success-mark{width:90px;height:90px;fill:#edf7ef;stroke:#39814c;stroke-width:4;animation:pop .45s ease-out}.success-mark path{fill:none;stroke-linecap:round;stroke-linejoin:round;stroke-dasharray:70;stroke-dashoffset:70;animation:tick .6s .2s forwards}.paid-amount{display:block;font-size:1.8rem;color:#254d33;margin:16px}.reference{font-size:.8rem}.spinner{width:48px;height:48px;border:4px solid #e3eae2;border-top-color:#39814c;border-radius:50%;margin:10px auto 24px;animation:spin 1s linear infinite}.state-icon{width:60px;height:60px;border-radius:50%;background:#fff1db;color:#8e5b21;display:grid;place-items:center;font-size:2rem;margin:auto}@keyframes tick{to{stroke-dashoffset:0}}@keyframes pop{from{transform:scale(.6);opacity:0}to{transform:scale(1);opacity:1}}@keyframes spin{to{transform:rotate(360deg)}}@media(prefers-reduced-motion:reduce){.success-mark,.success-mark path,.spinner{animation:none}.success-mark path{stroke-dashoffset:0}}`]
})
export class PagoEstadoComponent {
  @Input() estado = 'PENDIENTE'; @Input() mensaje = '';
  @Input() monto: number | null = null; @Input() idReserva = 0; @Input() transaccion = 0;
}
