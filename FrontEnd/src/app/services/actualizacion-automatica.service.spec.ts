import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { ActualizacionAutomaticaService } from './actualizacion-automatica.service';

describe('Actualización automática sin interrumpir formularios', () => {
  beforeEach(() => { vi.useFakeTimers(); TestBed.configureTestingModule({}); vi.spyOn(TestBed.inject(DOCUMENT), 'hidden', 'get').mockReturnValue(false); });
  afterEach(() => { document.querySelectorAll('dialog[data-prueba]').forEach(e => e.remove()); vi.restoreAllMocks(); vi.useRealTimers(); });
  it('consulta automáticamente y pausa las listas mientras hay un modal abierto', () => {
    const cambiar = vi.fn(); const sub = TestBed.inject(ActualizacionAutomaticaService).observar().subscribe(cambiar);
    vi.advanceTimersByTime(5000); expect(cambiar).toHaveBeenCalledTimes(1);
    const dialogo = document.createElement('dialog'); dialogo.setAttribute('open', ''); dialogo.dataset['prueba'] = 'modal'; document.body.append(dialogo);
    vi.advanceTimersByTime(10000); expect(cambiar).toHaveBeenCalledTimes(1);
    dialogo.remove(); vi.advanceTimersByTime(5000); expect(cambiar).toHaveBeenCalledTimes(2); sub.unsubscribe();
  });
  it('puede refrescar un detalle de solo lectura sin cerrar su modal', () => {
    const dialogo = document.createElement('dialog'); dialogo.setAttribute('open', ''); dialogo.dataset['prueba'] = 'detalle'; document.body.append(dialogo);
    const consultar = vi.fn(); const sub = TestBed.inject(ActualizacionAutomaticaService).observar(true).subscribe(consultar);
    vi.advanceTimersByTime(5000); expect(consultar).toHaveBeenCalledOnce(); expect(dialogo.hasAttribute('open')).toBe(true); sub.unsubscribe();
  });
});
