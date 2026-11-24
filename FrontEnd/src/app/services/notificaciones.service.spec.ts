import { NotificacionesService } from './notificaciones.service';

describe('Avisos temporales', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('desaparece a los cinco segundos y permite cierre manual', () => {
    const servicio = new NotificacionesService();
    servicio.mostrar('error', 'La fecha final no puede ser anterior a la inicial.');
    vi.advanceTimersByTime(4_999); expect(servicio.aviso()?.tipo).toBe('error');
    vi.advanceTimersByTime(1); expect(servicio.aviso()).toBeNull();
    servicio.mostrar('exito', 'Fechas bloqueadas.'); servicio.cerrar();
    expect(servicio.aviso()).toBeNull(); servicio.ngOnDestroy();
  });

  it('un error reemplaza el éxito anterior y conserva sus propios cinco segundos', () => {
    const servicio = new NotificacionesService();
    servicio.mostrar('exito', 'Guardado.'); vi.advanceTimersByTime(4_000);
    servicio.mostrar('error', 'Existe una reserva en ese rango.'); vi.advanceTimersByTime(1_000);
    expect(servicio.aviso()?.mensaje).toBe('Existe una reserva en ese rango.');
    vi.advanceTimersByTime(4_000); expect(servicio.aviso()).toBeNull(); servicio.ngOnDestroy();
  });
});
