import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MiReservaResponse, MisReservasPaginaResponse } from '../../models/reserva.model';
import { AuthService } from '../../services/auth';
import { MisReservasComponent } from './mis-reservas.component';

describe('Mis reservas del cliente', () => {
  let fixture: ComponentFixture<MisReservasComponent>;
  let http: HttpTestingController;
  const reserva: MiReservaResponse = {
    idReserva: 24, idCabana: 2, idUsuario: 5, nombreCabana: 'Jardín', imagenPrincipal: '/jardin.jpg',
    fechaIngreso: '2026-12-01', fechaSalida: '2026-12-03', cantidadHuespedes: 2,
    tarifaNocheAplicada: 100, montoTotal: 200, montoAdelanto: 100, saldoPendiente: 100,
    estadoReserva: 'CONFIRMADA', expiraEn: '2026-12-01T12:00:00', grupo: 'CONFIRMADAS', pagoDisponible: false
  };
  const respuesta = (content: MiReservaResponse[], page = 0, totalElements = content.length): MisReservasPaginaResponse =>
    ({ content, page, size: 20, totalElements, totalPages: Math.ceil(totalElements / 20) });
  const solicitud = () => http.expectOne(r => r.url.endsWith('/reservas/mis-reservas'));

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MisReservasComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), {
        provide: AuthService, useValue: {
          usuarioActual: signal({ nombre: 'Antonella', email: 'antonella@example.com', rol: 'CLIENTE' }),
          estaAutenticado: () => true
        }
      }]
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MisReservasComponent);
    fixture.detectChanges();
    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement;
    dialog.showModal = () => { dialog.open = true; };
    dialog.close = () => { dialog.open = false; };
  });

  afterEach(() => { fixture.destroy(); http.verify(); });

  it('carga la consulta personal sin enviar un ID de usuario', () => {
    const request = solicitud();
    expect(request.request.params.get('grupo')).toBe('TODAS');
    expect(request.request.params.has('idUsuario')).toBe(false);
    request.flush(respuesta([reserva]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Jardín');
    expect(fixture.nativeElement.querySelectorAll('app-pago-modal')).toHaveLength(0);
  });

  it('cancela la consulta anterior al filtrar y pagina en el servidor', () => {
    const anterior = solicitud();
    fixture.componentInstance.filtrar('CONFIRMADAS');
    expect(anterior.cancelled).toBe(true);
    const filtrada = solicitud();
    expect(filtrada.request.params.get('grupo')).toBe('CONFIRMADAS');
    filtrada.flush(respuesta([reserva], 0, 21));
    fixture.componentInstance.cambiarPagina(1);
    const siguiente = solicitud();
    expect(siguiente.request.params.get('page')).toBe('1');
    siguiente.flush(respuesta([reserva], 1, 21));
    fixture.componentInstance.filtrar('PASADAS');
    const historial = solicitud();
    expect(historial.request.params.get('page')).toBe('0');
    historial.flush(respuesta([]));
  });

  it('ofrece continuar pago únicamente cuando el backend indica que está disponible', () => {
    solicitud().flush(respuesta([
      { ...reserva, idReserva: 26, estadoReserva: 'PENDIENTE_PAGO', grupo: 'PENDIENTES', pagoDisponible: true },
      { ...reserva, idReserva: 18, estadoReserva: 'EXPIRADA', grupo: 'PASADAS' },
      reserva
    ]));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('app-pago-modal')).toHaveLength(1);
    expect(fixture.nativeElement.querySelector('app-pago-modal').textContent).toContain('Continuar pago');
    expect(fixture.nativeElement.textContent).toContain('Historial');
    expect(fixture.componentInstance.pagado({ ...reserva, estadoReserva: 'PENDIENTE_PAGO' })).toBe(0);
  });

  it('abre y cierra el detalle de una reserva propia sin consultar el endpoint del guardián', () => {
    solicitud().flush(respuesta([reserva]));
    fixture.detectChanges();
    fixture.nativeElement.querySelector('[aria-label="Ver detalle de reserva #24"]').click();
    fixture.detectChanges();
    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement;
    expect(dialog.open).toBe(true);
    expect(dialog.textContent).toContain('Jardín');
    expect(dialog.textContent).toContain('Saldo pendiente');
    dialog.dispatchEvent(new Event('cancel', { cancelable: true }));
    fixture.detectChanges();
    expect(dialog.open).toBe(false);
    http.expectNone(r => !r.url.endsWith('/mis-reservas'));
  });

  it('distingue un error de un historial vacío y permite reintentar', () => {
    solicitud().flush({}, { status: 503, statusText: 'Service Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No pudimos cargar tus reservas');
    expect(fixture.nativeElement.textContent).not.toContain('Tu próxima escapada te espera');
    fixture.componentInstance.cargar();
    solicitud().flush(respuesta([reserva]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Jardín');
  });
});
