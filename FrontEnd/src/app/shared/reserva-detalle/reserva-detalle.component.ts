import { FormsModule } from '@angular/forms';
import { PagosService, PagoRestanteRequest } from '../../services/pagos.service';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { afterNextRender, Component, DestroyRef, ElementRef, inject, input, OnInit, output, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import {
  CheckoutRequest, ESTADOS_RESERVA, MEDIOS_PAGO_DANOS, MedioPagoDanos, ReservaDetalleResponse
} from '../../models/reserva.model';
import { ReservasService } from '../../services/reservas.service';
@Component({
  selector: 'app-reserva-detalle',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reserva-detalle.component.html',
  styleUrl: './reserva-detalle.component.css'
})
export class ReservaDetalleComponent implements OnInit {
  private readonly reservasService = inject(ReservasService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  readonly idReserva = input.required<number>();
  readonly cerrar = output<void>();
  readonly actualizado = output<void>();
  private readonly pagosService = inject(PagosService);
  readonly guardando = signal(false);
  readonly errorCheckout = signal('');
  readonly exitoCheckout = signal('');
  readonly mediosPagoDanos = MEDIOS_PAGO_DANOS;
  readonly formatoMontoDanos = /^\d{1,8}([.,]\d{1,2})?$/;
  huboDanos = false;
  descripcionDanos = '';
  montoDanos = '';
  medioPagoDanos: MedioPagoDanos = 'EFECTIVO';
  readonly consultandoPago = signal(true);
  readonly errorPago = signal('');
  readonly exitoPago = signal('');
  readonly registroExistente = signal(false);
  readonly errorConsultaPago = signal(false);
  metodoPago: PagoRestanteRequest['metodoPago'] = 'EFECTIVO';
  numeroOperacion = '';
  private claveIdempotencia: string = crypto.randomUUID();
  consultarPago(limpiarError = true): void {
    this.consultandoPago.set(true);
    if (limpiarError) this.errorPago.set('');
    this.errorConsultaPago.set(false);
    this.pagosService.consultarPagoRestante(this.idReserva()).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.consultandoPago.set(false))
    ).subscribe({
      next: pago => {
        this.registroExistente.set(!!pago);
        if (pago) {
          this.registroExistente.set(true);
          this.metodoPago = pago.metodoPago;
          this.numeroOperacion = pago.numeroOperacion ?? '';
          this.claveIdempotencia = pago.claveIdempotencia;
          if (pago.completado) this.exitoPago.set('Pago restante registrado y check-in completado. Cabaña ocupada.');
        }
      },
      error: () => {
        this.errorConsultaPago.set(true);
        this.errorPago.set('No se pudo consultar el registro de pago. Reintenta antes de continuar.');
      }
    });
  }
  registrarPago(): void {
    if (this.guardando() || this.consultandoPago() || this.errorConsultaPago()) return;
    const numero = this.numeroOperacion.trim();
    if (this.metodoPago !== 'EFECTIVO' && !numero) {
      this.errorPago.set('Ingresa el número de operación.'); return;
    }
    if (numero.length > 100) { this.errorPago.set('El número de operación admite hasta 100 caracteres.'); return; }
    this.guardando.set(true);
    this.errorPago.set('');
    this.pagosService.registrarPagoRestante(this.idReserva(), {
      metodoPago: this.metodoPago, numeroOperacion: numero || null, claveIdempotencia: this.claveIdempotencia
    }).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.guardando.set(false))).subscribe({
      next: () => {
        this.exitoPago.set('Pago restante registrado y check-in completado. Cabaña ocupada.');
        this.registroExistente.set(true);
        this.actualizado.emit();
        this.cargarDetalle();
      },
      error: (error: unknown) => {
        this.errorPago.set(error instanceof HttpErrorResponse
          ? error.error?.detail ?? (error.status === 403 ? 'Tu cuenta no tiene permiso para registrar el check-in.'
            : 'No se pudo completar la operación. Reintenta: el pago no se duplicará.')
          : 'La operación tardó demasiado. Reintenta: el pago no se duplicará.');
        // Bloquea la edición tras un resultado incierto; el reintento conserva los mismos datos.
        this.registroExistente.set(!(error instanceof HttpErrorResponse) || error.status === 0 || error.status >= 500 || error.status === 409);
        this.consultarPago(false);
      }
    });
  }
  registrarCheckout(): void {
    const reserva = this.detalle();
    if (!reserva || reserva.estadoReserva !== 'OCUPADA' || this.guardando()) return;

    const request: CheckoutRequest = {};
    if (this.huboDanos) {
      const descripcion = this.descripcionDanos.trim();
      const montoTexto = this.montoDanos.trim();
      const monto = Number(montoTexto.replace(',', '.'));
      if (!descripcion || descripcion.length > 500 || !this.formatoMontoDanos.test(montoTexto) || monto <= 0) {
        this.errorCheckout.set('Ingresa una descripción y un importe mayor que cero con hasta dos decimales.');
        return;
      }
      request.danos = { descripcion, monto, medioPago: this.medioPagoDanos };
    }

    this.errorCheckout.set('');
    this.guardando.set(true);
    this.reservasService.registrarCheckout(reserva.idReserva, request).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.guardando.set(false))
    ).subscribe({
      next: ({ estadoReserva, checkout }) => {
        this.detalle.set({ ...reserva, estadoReserva, checkout });
        this.exitoPago.set('');
        this.exitoCheckout.set('Check-out registrado. La cabaña quedó disponible.');
        this.actualizado.emit();
      },
      error: (error: unknown) => {
        this.errorCheckout.set(error instanceof HttpErrorResponse && error.status === 403
          ? 'Tu cuenta no tiene permiso para registrar el check-out.'
          : error instanceof HttpErrorResponse && error.status === 409
            ? 'La reserva cambió. Cierra y vuelve a abrir el detalle para consultar su estado.'
            : 'No se pudo confirmar el check-out. Reintenta con los mismos datos.');
      }
    });
  }

  readonly detalle = signal<ReservaDetalleResponse | null>(null);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly estados = ESTADOS_RESERVA;
  constructor() {
    afterNextRender(() => this.dialog().nativeElement.showModal());
  }
  ngOnInit(): void {
    this.cargarDetalle();
    this.consultarPago();
  }
  cargarDetalle(): void {
    this.cargando.set(true);
    this.error.set('');
    this.detalle.set(null);
    this.reservasService.consultarDetalle(this.idReserva()).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.cargando.set(false))
    ).subscribe({
      next: detalle => this.detalle.set(detalle),
      error: (error: unknown) => {
        const status = error instanceof HttpErrorResponse ? error.status : 0;
        this.error.set(status === 404 ? 'Esta reserva ya no está disponible.'
          : status === 401 ? 'Tu sesión expiró. Vuelve a iniciar sesión.'
          : status === 403 ? 'Tu cuenta no tiene permiso para consultar esta reserva.'
          : 'No se pudo cargar el detalle. Intenta nuevamente.');
      }
    });
  }
  cerrarModal(): void {
    if (this.guardando()) return;
    this.dialog().nativeElement.close();
    this.cerrar.emit();
  }
  cerrarDesdeFondo(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement) this.cerrarModal();
  }
}
