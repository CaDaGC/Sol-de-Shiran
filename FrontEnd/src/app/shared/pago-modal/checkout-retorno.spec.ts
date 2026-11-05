import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { CheckoutRetornoService, CheckoutRetorno } from '../../services/checkout-retorno.service';
import { PagoModalComponent } from './pago-modal.component';
import { PagoResultadoComponent } from './pago-resultado.component';
import { AuthService } from '../../services/auth';
import { PagosService } from '../../services/pagos.service';

const reserva = { idReserva: 8, idCabana: 3, idUsuario: 1, fechaIngreso: '2026-12-01', fechaSalida: '2026-12-02',
  cantidadHuespedes: 2, tarifaNocheAplicada: 250, montoTotal: 250, montoAdelanto: 125, saldoPendiente: 125,
  estadoReserva: 'PENDIENTE_PAGO', expiraEn: '2099-12-01T12:00:00' };
const contexto = (): CheckoutRetorno => ({ transaccion: 7, reserva, nombreCabana: 'Familiar', url: '/catalogo/3?adultos=2',
  adultos: 2, ninos: 0, imagen: 1, scroll: 300, fecha: Date.now() });

describe('Retorno de Mercado Pago', () => {
  afterEach(() => { sessionStorage.clear(); TestBed.resetTestingModule(); });
  it('conserva la reserva y rechaza retornos externos, vencidos o de otra operación', () => {
    const service = new CheckoutRetornoService(); service.guardar(contexto());
    expect(service.obtener(7)?.reserva.idReserva).toBe(8);
    expect(service.obtener(9)).toBeNull();
    service.guardar({ ...contexto(), url: 'https://example.com/catalogo/3' }); expect(service.obtener(7)).toBeNull();
    service.guardar({ ...contexto(), fecha: Date.now() - 86400001 }); expect(service.obtener(7)).toBeNull();
  });
  it('el retorno vuelve al detalle guardado conservando filtros y operación', () => {
    const service = new CheckoutRetornoService(); service.guardar(contexto());
    const navigate = vi.fn().mockResolvedValue(true);
    const tree = { queryParams: { adultos: '2' } };
    TestBed.configureTestingModule({ providers: [
      { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: new Map([['transaccion','7']]) } } },
      { provide: Router, useValue: { parseUrl: vi.fn().mockReturnValue(tree), navigateByUrl: navigate } },
      { provide: PagosService, useValue: { consultarEstado: vi.fn() } },
      { provide: CheckoutRetornoService, useValue: service }
    ] });
    const component = TestBed.runInInjectionContext(() => new PagoResultadoComponent(TestBed.inject(ActivatedRoute), TestBed.inject(PagosService)));
    component.ngOnInit();
    expect(navigate).toHaveBeenCalledWith({ queryParams: { adultos: '2', transaccion: 7 } }, { replaceUrl: true });
    component.ngOnDestroy();
  });
  it('solo muestra aprobado tras verificar el servidor; conserva el modal abierto', async () => {
    const response = new Subject<any>();
    TestBed.configureTestingModule({ imports: [PagoModalComponent], providers: [
      { provide: PagosService, useValue: { consultarEstado: () => response } },
      { provide: AuthService, useValue: { estaAutenticado: () => true } },
      { provide: Router, useValue: {} }
    ] });
    const fixture = TestBed.createComponent(PagoModalComponent);
    fixture.componentRef.setInput('reserva', reserva); fixture.componentRef.setInput('transaccionRetorno', 7);
    const confirmed = vi.fn(); fixture.componentInstance.confirmado.subscribe(confirmed);
    fixture.detectChanges(); expect(confirmed).not.toHaveBeenCalled();
    expect(fixture.componentInstance.abierto()).toBe(true);
    response.next({ idReserva: 8, estado: 'APROBADA' }); response.complete();
    await fixture.whenStable(); fixture.detectChanges();
    expect(confirmed).toHaveBeenCalledOnce();
    expect(fixture.nativeElement.textContent).toContain('¡Tu reserva está confirmada!');
    expect(fixture.nativeElement.querySelector('svg.success-mark')).not.toBeNull(); fixture.destroy();
  });
  it('un pago de otra reserva nunca activa la confirmación', async () => {
    TestBed.configureTestingModule({ imports: [PagoModalComponent], providers: [
      { provide: PagosService, useValue: { consultarEstado: () => of({ idReserva: 99, estado: 'APROBADA' }) } },
      { provide: AuthService, useValue: {} }, { provide: Router, useValue: {} }
    ] });
    const fixture = TestBed.createComponent(PagoModalComponent); fixture.componentRef.setInput('reserva',reserva);
    fixture.componentRef.setInput('transaccionRetorno',7); const confirmed=vi.fn();fixture.componentInstance.confirmado.subscribe(confirmed);
    fixture.detectChanges(); await fixture.whenStable();
    expect(confirmed).not.toHaveBeenCalled(); expect(fixture.componentInstance.estadoPago()).toBe('ERROR');fixture.destroy();
  });
  it('un retorno sin contexto sigue consultando el servidor y no confía en la URL', async () => {
    const consultar = vi.fn().mockReturnValue(of({ idReserva: 8, idTransaccion: 7, monto: 125, estado: 'PENDIENTE' }));
    TestBed.configureTestingModule({ providers: [
      { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: new Map([['transaccion','7'],['status','approved']]) } } },
      { provide: Router, useValue: {} }, { provide: PagosService, useValue: { consultarEstado: consultar } }
    ] });
    const component=TestBed.runInInjectionContext(() => new PagoResultadoComponent(TestBed.inject(ActivatedRoute),TestBed.inject(PagosService)));
    await component.consultar(); expect(consultar).toHaveBeenCalledWith(7); expect(component.estado()).toBe('PENDIENTE');component.ngOnDestroy();
  });
});
