import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ReservaDetalleComponent } from './reserva-detalle.component';

describe('HU7 pago restante y check-in', () => {
  let fixture: ComponentFixture<ReservaDetalleComponent>;
  let http: HttpTestingController;
  const reserva = { idReserva: 3, idCabana: 4, estadoReserva: 'CONFIRMADA', saldoPendiente: 125,
    montoTotal: 250, montoAdelanto: 125, fechaIngreso: '2026-11-05', fechaSalida: '2026-11-06' };
  beforeEach(() => {
    Object.defineProperty(HTMLDialogElement.prototype, 'showModal', { configurable: true, value() {} });
    TestBed.configureTestingModule({ imports: [ReservaDetalleComponent], providers: [provideHttpClient(), provideHttpClientTesting()] });
    fixture = TestBed.createComponent(ReservaDetalleComponent);
    fixture.componentRef.setInput('idReserva', 3);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne(r => r.url.endsWith('/reservas/panel/3')).flush(reserva);
  });
  afterEach(() => { fixture.destroy(); http.verify(); });
  it('exige referencia para tarjeta y billetera antes de enviar', () => {
    http.expectOne(r => r.url.endsWith('/restante')).flush(null);
    for (const metodo of ['TARJETA', 'YAPE_BILLETERA'] as const) {
      fixture.componentInstance.metodoPago = metodo;
      fixture.componentInstance.numeroOperacion = '   ';
      fixture.componentInstance.registrarPago();
      expect(fixture.componentInstance.errorPago()).toContain('número de operación');
      http.expectNone(r => r.method === 'POST');
    }
  });
  it('efectivo envía saldo calculado en servidor y bloquea doble envío; éxito refresca el detalle', () => {
    http.expectOne(r => r.url.endsWith('/restante')).flush(null);
    const actualizado = vi.fn(); fixture.componentInstance.actualizado.subscribe(actualizado);
    fixture.componentInstance.registrarPago(); fixture.componentInstance.registrarPago();
    const post = http.expectOne(r => r.method === 'POST');
    expect(post.request.body.metodoPago).toBe('EFECTIVO');
    expect(post.request.body.numeroOperacion).toBeNull();
    expect(post.request.body.monto).toBeUndefined();
    post.flush({ completado: true });
    http.expectOne(r => r.url.endsWith('/reservas/panel/3')).flush({ ...reserva, saldoPendiente: 0, estadoReserva: 'OCUPADA' });
    expect(actualizado).toHaveBeenCalledOnce(); expect(fixture.componentInstance.exitoPago()).toContain('completado');
    fixture.detectChanges(); expect(fixture.nativeElement.querySelector('#check-in-title')).toBeNull();
  });
  it('recupera registro pendiente y reintenta con la misma clave', () => {
    const clave = crypto.randomUUID();
    http.expectOne(r => r.url.endsWith('/restante')).flush({ metodoPago: 'TARJETA', numeroOperacion: 'ABC', claveIdempotencia: clave, completado: false });
    fixture.componentInstance.registrarPago();
    const post = http.expectOne(r => r.method === 'POST');
    expect(post.request.body.claveIdempotencia).toBe(clave);
    post.flush({ detail: 'Reintenta' }, { status: 503, statusText: 'Unavailable' });
    http.expectOne(r => r.method === 'GET' && r.url.endsWith('/restante')).flush({ metodoPago: 'TARJETA', numeroOperacion: 'ABC', claveIdempotencia: clave, completado: false });
    expect(fixture.componentInstance.registroExistente()).toBe(true);
    expect(fixture.componentInstance.guardando()).toBe(false);
    fixture.componentInstance.registrarPago();
    const retry = http.expectOne(r => r.method === 'POST');
    expect(retry.request.body).toEqual(post.request.body);
    retry.flush({}, { status: 503, statusText: 'Unavailable' });
    http.expectOne(r => r.method === 'GET' && r.url.endsWith('/restante')).flush({ metodoPago: 'TARJETA', numeroOperacion: 'ABC', claveIdempotencia: clave, completado: false });
  });
  it('un conflicto de fechas sin pago conserva el error y permite editar el formulario', () => {
    http.expectOne(r => r.url.endsWith('/restante')).flush(null);
    fixture.componentInstance.registrarPago();
    http.expectOne(r => r.method === 'POST').flush({ detail: 'Check-in disponible desde el 27/11/2026' },
      { status: 409, statusText: 'Conflict' });
    http.expectOne(r => r.method === 'GET' && r.url.endsWith('/restante')).flush(null);
    expect(fixture.componentInstance.registroExistente()).toBe(false);
    expect(fixture.componentInstance.errorPago()).toContain('27/11/2026');
  });
  it('un 503 sin pago persistido permite volver a elegir el método', () => {
    http.expectOne(r => r.url.endsWith('/restante')).flush(null);
    fixture.componentInstance.registrarPago();
    http.expectOne(r => r.method === 'POST').flush({}, { status: 503, statusText: 'Unavailable' });
    http.expectOne(r => r.method === 'GET' && r.url.endsWith('/restante')).flush(null);
    expect(fixture.componentInstance.registroExistente()).toBe(false);
  });
  it('bloquea envío si no pudo consultar el pago existente' , () => {
    http.expectOne(r => r.url.endsWith('/restante')).flush({}, { status: 503, statusText: 'Unavailable' });
    fixture.componentInstance.registrarPago(); http.expectNone(r => r.method === 'POST');
  });
});

describe('HU9 registro de check-out', () => {
  let fixture: ComponentFixture<ReservaDetalleComponent>;
  let http: HttpTestingController;
  const reserva = {
    idReserva: 9, idCabana: 4, estadoReserva: 'OCUPADA', saldoPendiente: 0,
    montoTotal: 250, montoAdelanto: 125, fechaIngreso: '2026-11-05', fechaSalida: '2026-11-06'
  };

  beforeEach(async () => {
    Object.defineProperty(HTMLDialogElement.prototype, 'showModal', { configurable: true, value() {} });
    TestBed.configureTestingModule({
      imports: [ReservaDetalleComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    fixture = TestBed.createComponent(ReservaDetalleComponent);
    fixture.componentRef.setInput('idReserva', 9);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne(r => r.url.endsWith('/reservas/panel/9')).flush(reserva);
    http.expectOne(r => r.url.endsWith('/restante')).flush(null);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => { fixture.destroy(); http.verify(); });

  it('registra salida sin cobro y notifica al panel una sola vez', () => {
    const actualizado = vi.fn();
    fixture.componentInstance.actualizado.subscribe(actualizado);
    const root: HTMLElement = fixture.nativeElement;
    expect(root.querySelector('#monto-danos')).toBeNull();

    const form = root.querySelector('form')!;
    form.dispatchEvent(new Event('submit', { cancelable: true }));
    form.dispatchEvent(new Event('submit', { cancelable: true }));
    const post = http.expectOne(r => r.method === 'POST' && r.url.endsWith('/9/check-out'));
    expect(post.request.body).toEqual({});
    post.flush({
      estadoReserva: 'FINALIZADA',
      checkout: { fecha: '2026-11-06T10:00:00', descripcionDanos: null, montoDanos: null, medioPago: null }
    });
    fixture.detectChanges();
    expect(actualizado).toHaveBeenCalledOnce();
    expect(root.querySelector('form')).toBeNull();
    expect(root.textContent).toContain('Sin cobro');
  });

  it('muestra los campos de daños y envía un cobro presencial válido', async () => {
    const root: HTMLElement = fixture.nativeElement;
    root.querySelector<HTMLInputElement>('input[type=checkbox]')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    const component = fixture.componentInstance;
    expect(root.querySelector('#monto-danos')).toBeTruthy();

    component.registrarCheckout();
    http.expectNone(r => r.method === 'POST');
    component.descripcionDanos = '  Toalla dañada  ';
    component.montoDanos = '25,501';
    component.registrarCheckout();
    http.expectNone(r => r.method === 'POST');

    component.montoDanos = '25,50';
    component.medioPagoDanos = 'YAPE';
    component.registrarCheckout();
    const post = http.expectOne(r => r.method === 'POST' && r.url.endsWith('/9/check-out'));
    expect(post.request.body).toEqual({
      danos: { descripcion: 'Toalla dañada', monto: 25.5, medioPago: 'YAPE' }
    });
    post.flush({
      estadoReserva: 'FINALIZADA',
      checkout: { fecha: '2026-11-06T10:00:00', descripcionDanos: 'Toalla dañada', montoDanos: 25.5, medioPago: 'YAPE' }
    });
    fixture.detectChanges();
    expect(root.textContent).toContain('Toalla dañada');
    expect(root.textContent).toContain('Yape');
  });
});
