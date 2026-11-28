import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { PedidoRestaurante } from '../../../models/restaurante.model';
import { PanelRestauranteComponent } from './panel-restaurante.component';
import { AuthService } from '../../../services/auth';
import { UsuariosService } from '../../../services/usuarios.service';
import { RestauranteService } from '../../../services/restaurante.service';
import { PedidosRestauranteService } from '../../../services/pedidos-restaurante.service';

const local = { idRestaurante: 2, nombre: 'El Campestre', estado: 'ACTIVO', horarioAtencion: '08:00 a 22:00' };
const pedido: PedidoRestaurante = { idReserva: 1, fechaEntrega: null, motivoRechazo: null, idPedido: 4, codigoPedido: 'PED00004', idRestaurante: 2, nombreRestaurante: 'El Campestre', nombreHuesped: 'Carlos', nombreCabana: 'Cabaña Luna', montoTotal: 25, estadoPedido: 'NUEVO', estadoPago: 'PENDIENTE', metodoPagoPresencial: 'EFECTIVO', fechaPedido: '2026-11-27T12:00:00', detalles: [{ idProducto: 8, nombreProducto: 'Plato', cantidad: 1, precioUnitario: 25, subtotal: 25, observaciones: 'Sin ají' }] };

describe('Panel del personal de restaurante', () => {
  const originales = ['showModal', 'close'].map(k => Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, k));
  beforeAll(() => {
    Object.defineProperty(HTMLDialogElement.prototype, 'showModal', { configurable: true, value(this: HTMLDialogElement) { this.open = true; } });
    Object.defineProperty(HTMLDialogElement.prototype, 'close', { configurable: true, value(this: HTMLDialogElement) { this.open = false; } });
  });
  afterAll(() => ['showModal', 'close'].forEach((k, i) => { if (originales[i]) Object.defineProperty(HTMLDialogElement.prototype, k, originales[i]!); else Reflect.deleteProperty(HTMLDialogElement.prototype, k); }));
  const api = {
    panel: vi.fn(), detalle: vi.fn(), cambiarEstado: vi.fn(), comanda: vi.fn(),
  };
  beforeEach(() => {
    api.panel.mockReset().mockReturnValue(of({ restaurante: local, resumen: { nuevos: 1, enPreparacion: 0, enEntrega: 0, finalizadosHoy: 0 }, pedidos: [pedido], totalElementos: 1, pagina: 0, totalPaginas: 1, primera: true, ultima: true }));
    api.detalle.mockReset().mockReturnValue(of(pedido));
    api.cambiarEstado.mockReset().mockReturnValue(of({ ...pedido, estadoPedido: 'EN_PREPARACION' }));
    TestBed.configureTestingModule({ imports: [PanelRestauranteComponent], providers: [provideRouter([]),
      { provide: AuthService, useValue: { usuarioActual: signal({ rol: 'RESTAURANTE' }) } },
      { provide: UsuariosService, useValue: { perfil: () => of({ id: 7 }) } },
      { provide: RestauranteService, useValue: { miLocal: () => of(local) } },
      { provide: PedidosRestauranteService, useValue: api },
    ] });
  });
  it('muestra tabla con código, huésped, cabaña y administración', async () => {
    const f = TestBed.createComponent(PanelRestauranteComponent); f.detectChanges(); await f.whenStable(); f.detectChanges();
    expect(f.nativeElement.textContent).toContain('PED00004');
    expect(f.nativeElement.textContent).toContain('Cabaña Luna');
    expect(f.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1);
    f.destroy();
  });
  it('exige confirmación antes de aceptar y conserva observaciones en el detalle', async () => {
    const f = TestBed.createComponent(PanelRestauranteComponent); f.detectChanges(); await f.whenStable();
    f.componentInstance.abrir(pedido); f.detectChanges(); await f.whenStable(); f.detectChanges();
    expect(f.nativeElement.textContent).toContain('Sin ají');
    const aceptar = [...f.nativeElement.querySelectorAll('button')].find((b: HTMLButtonElement) => b.textContent?.trim() === 'Aceptar pedido') as HTMLButtonElement;
    aceptar.click(); expect(api.cambiarEstado).not.toHaveBeenCalled();
    const confirmar = document.querySelector<HTMLButtonElement>('.confirmacion-escritura .btn-success');
    expect(confirmar).not.toBeNull(); confirmar!.click();
    expect(api.cambiarEstado).toHaveBeenCalledWith(2, 4, 'EN_PREPARACION', undefined);
    f.destroy();
  });
});
