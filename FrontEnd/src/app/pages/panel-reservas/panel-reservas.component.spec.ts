import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CabanaService } from '../../services/cabana.service';
import { ReservaPanelResponse, ReservasPaginaResponse } from '../../models/reserva.model';
import { PanelReservasComponent } from './panel-reservas.component';

describe('Integración del panel de reservas', () => {
  const metodosDialog = ['showModal', 'close'] as const;
  const originalesDialog = metodosDialog.map(metodo => Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, metodo));

  // JSDOM no implementa los métodos del diálogo nativo del navegador.
  beforeAll(() => {
    metodosDialog.forEach(metodo => Object.defineProperty(HTMLDialogElement.prototype, metodo, {
      configurable: true,
      value(this: HTMLDialogElement) { this.open = metodo === 'showModal'; }
    }));
  });
  afterAll(() => {
    metodosDialog.forEach((metodo, indice) => {
      const original = originalesDialog[indice];
      if (original) Object.defineProperty(HTMLDialogElement.prototype, metodo, original);
      else Reflect.deleteProperty(HTMLDialogElement.prototype, metodo);
    });
  });
  let fixture: ComponentFixture<PanelReservasComponent>;
  let http: HttpTestingController;
  const esperar = (ms = 0) => new Promise(resolve => setTimeout(resolve, ms));
  const reserva: ReservaPanelResponse = {
    idReserva: 1, idUsuario: 5, nombreUsuario: 'Antonella', emailUsuario: 'antonella@example.com',
    idCabana: 2, nombreCabana: 'Familiar', fechaIngreso: '2026-12-01', fechaSalida: '2026-12-03',
    cantidadHuespedes: 2, montoTotal: 200, montoAdelanto: 100, saldoPendiente: 100,
    estadoReserva: 'CONFIRMADA'
  };
  function pagina(content: ReservaPanelResponse[] = [], page = 0, totalElements = content.length): ReservasPaginaResponse {
    return { content, page, size: 20, totalElements, totalPages: Math.ceil(totalElements / 20) };
  }
  const solicitud = () => http.expectOne(req => req.url.endsWith('/reservas'));

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [PanelReservasComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(),
        { provide: CabanaService, useValue: { listarCabanas: () => of([{ idCabana: 2, nombre: 'Familiar' }]) } }]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PanelReservasComponent);
    fixture.detectChanges();
    await esperar();
  });

  afterEach(() => {
    fixture.destroy();
    http.verify();
  });

  it('muestra carga antes de la respuesta y luego las reservas confirmadas', () => {
    expect(fixture.nativeElement.textContent).toContain('Cargando reservas');
    expect(fixture.nativeElement.textContent).not.toContain('No hay reservas para mostrar');
    const request = solicitud();
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.has('buscar')).toBe(false);
    expect(request.request.params.has('idCabana')).toBe(false);
    request.flush(pagina([reserva, { ...reserva, idReserva: 2 }]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('2 reservas');
    expect(fixture.nativeElement.querySelectorAll('.reservation-list [data-status="CONFIRMADA"]').length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Familiar');
  });

  it('seleccionar una reserva actualiza noches e importes sin otra petición', () => {
    solicitud().flush(pagina([reserva, { ...reserva, idReserva: 2, nombreCabana: 'Jardín',
      fechaSalida: '2026-12-05', montoTotal: 900, saldoPendiente: 150 }]));
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button[aria-label="Seleccionar reserva #2"]').click();
    fixture.detectChanges();
    const detalle = fixture.nativeElement.querySelector('.detail-card');
    expect(detalle.textContent).toContain('Jardín');
    expect(detalle.textContent).toContain('4 noches');
    expect(detalle.querySelector('.amount-grid > div:nth-child(2)').textContent).toContain('750.00');
    expect(detalle.querySelector('.balance').textContent).toContain('150.00');
    http.expectNone(req => req.url.includes('/reservas'));
  });

  it('cancela respuestas anteriores y combina búsqueda y cabaña en el servidor', async () => {
    const anterior = solicitud();
    fixture.componentInstance.actualizarBusqueda('incorrecto');
    fixture.componentInstance.actualizarBusqueda('  Antonella  ');
    expect(anterior.cancelled).toBe(true);
    await esperar(350);
    const busqueda = solicitud();
    expect(busqueda.request.params.get('buscar')).toBe('Antonella');
    busqueda.flush(pagina());
    fixture.componentInstance.filtrarCabana(2);
    await esperar();
    const combinada = solicitud();
    expect(combinada.request.params.get('idCabana')).toBe('2');
    expect(combinada.request.params.get('buscar')).toBe('Antonella');
    combinada.flush(pagina([reserva]));
  });

  it('pagina en el servidor y vuelve a la primera página al limpiar filtros', async () => {
    solicitud().flush(pagina([reserva], 0, 24));
    fixture.componentInstance.cambiarPagina(1);
    await esperar();
    const segunda = solicitud();
    expect(segunda.request.params.get('page')).toBe('1');
    segunda.flush(pagina([reserva], 1, 24));
    fixture.componentInstance.limpiarFiltros();
    await esperar();
    const primera = solicitud();
    expect(primera.request.params.get('page')).toBe('0');
    primera.flush(pagina([reserva], 0, 24));
  });

  it('consulta y muestra el detalle seleccionado al pulsar Administrar', () => {
    solicitud().flush(pagina([reserva]));
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button[aria-label="Administrar reserva #1"]').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('dialog').textContent).toContain('Cargando detalle');
    http.expectOne(req => req.url.endsWith('/reservas/1')).flush({
      ...reserva, nombreUsuario: 'Nombre actualizado', tarifaNocheAplicada: 100,
      expiraEn: '2026-11-05T12:15:00', fechaCreacion: '2026-11-05T12:00:00'
    });
    fixture.detectChanges();
    const modal = fixture.nativeElement.querySelector('dialog');
    expect(modal.textContent).toContain('Nombre actualizado');
    expect(modal.textContent).toContain('antonella@example.com');
    expect(modal.textContent).toContain('Saldo pendiente');
    modal.querySelector('[aria-label="Cerrar detalle de reserva"]').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('dialog')).toBeNull();
    expect(fixture.componentInstance.reservas()).toHaveLength(1);
  });

  it('cerrar durante la carga cancela la consulta y permite abrir otra reserva', () => {
    solicitud().flush(pagina([reserva, { ...reserva, idReserva: 2 }]));
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button[aria-label="Administrar reserva #1"]').click();
    fixture.detectChanges();
    const pendiente = http.expectOne(req => req.url.endsWith('/reservas/1'));
    fixture.nativeElement.querySelector('dialog').dispatchEvent(new Event('cancel', { cancelable: true }));
    fixture.detectChanges();
    expect(pendiente.cancelled).toBe(true);
    fixture.nativeElement.querySelector('button[aria-label="Seleccionar reserva #2"]').click();
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button[aria-label="Administrar reserva #2"]').click();
    fixture.detectChanges();
    http.expectOne(req => req.url.endsWith('/reservas/2')).flush({
      ...reserva, idReserva: 2, tarifaNocheAplicada: 100, expiraEn: '2026-11-05T12:15:00', fechaCreacion: null
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('dialog').textContent).toContain('Reserva #2');
  });

  it('muestra el 404 del detalle y puede reintentar sin afectar el listado', () => {
    solicitud().flush(pagina([reserva]));
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button[aria-label="Administrar reserva #1"]').click();
    fixture.detectChanges();
    http.expectOne(req => req.url.endsWith('/reservas/1'))
      .flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    const modal = fixture.nativeElement.querySelector('dialog');
    expect(modal.textContent).toContain('Esta reserva ya no está disponible');
    modal.querySelector('.message button').click();
    http.expectOne(req => req.url.endsWith('/reservas/1')).flush({
      ...reserva, tarifaNocheAplicada: 100, expiraEn: '2026-11-05T12:15:00', fechaCreacion: null
    });
    fixture.detectChanges();
    expect(modal.textContent).toContain('Antonella');
    expect(fixture.componentInstance.resultado()?.totalElements).toBe(1);
  });

  it('un error no se presenta como cero reservas y permite reintentar', async () => {
    solicitud().flush({}, { status: 503, statusText: 'Service Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No se pudo cargar el registro');
    expect(fixture.nativeElement.textContent).not.toContain('No hay reservas para mostrar');
    fixture.componentInstance.cargar();
    await esperar();
    solicitud().flush(pagina([reserva]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Confirmada');
    expect(fixture.componentInstance.error()).toBe('');
  });
});
