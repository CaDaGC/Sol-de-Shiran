import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { CatalogoComponent } from './catalogo.component';
import { CabanaService } from '../../services/cabana.service';
import { ReservasService } from '../../services/reservas.service';
import { AuthService } from '../../services/auth';

describe('Buscador del catálogo', () => {
  const cabanas = [1, 2, 3].map((id) => ({
    idCabana: id,
    nombre: `Cabaña ${id}`,
    descripcion: '',
    aforoMaximo: id === 1 ? 2 : 6,
    numHabitaciones: 1,
    numBanos: 1,
    tarifaNoche: 100,
    imagenPrincipal: '',
  }));
  let componente: CatalogoComponent;
  const consultar = vi.fn();
  beforeEach(() => {
    consultar.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: CabanaService, useValue: {} },
        { provide: ReservasService, useValue: { consultarDisponibilidad: consultar } },
        { provide: AuthService, useValue: { usuarioActual: signal(null) } },
      ],
    });
    componente = TestBed.runInInjectionContext(
      () => new CatalogoComponent(TestBed.inject(CabanaService)),
    );
    componente.cabanas.set(cabanas);
    componente.fechaIngreso = '2099-01-01';
    componente.fechaSalida = '2099-01-03';
    componente.huespedes = 4;
  });
  it('filtra por aforo y disponibilidad, conservando los parámetros para el detalle', () => {
    consultar.mockImplementation((id: number) => of({ disponible: id === 3 }));
    componente.buscarDisponibilidad();
    expect(consultar).toHaveBeenCalledTimes(2);
    expect(componente.visibles().map((c) => c.idCabana)).toEqual([3]);
    expect(componente.parametros).toEqual({
      fechaIngreso: '2099-01-01',
      fechaSalida: '2099-01-03',
      adultos: 4,
      ninos: 0,
    });
    componente.limpiarBusqueda();
    expect(componente.visibles()).toEqual(cabanas);
  });
  it('muestra un resultado vacío cuando ninguna cabaña tiene capacidad suficiente', () => {
    componente.huespedes = 10;
    componente.buscarDisponibilidad();
    expect(consultar).not.toHaveBeenCalled();
    expect(componente.filtrado()).toBe(true);
    expect(componente.visibles()).toEqual([]);
    expect(componente.errorBusqueda).toBe('');
  });
  it('rechaza un rango inválido sin consultar servicios', () => {
    componente.fechaSalida = componente.fechaIngreso;
    componente.buscarDisponibilidad();
    expect(consultar).not.toHaveBeenCalled();
    expect(componente.errorBusqueda).toBeTruthy();
  });
  it('distingue un error de servicio de un resultado sin disponibilidad', () => {
    consultar.mockReturnValue(throwError(() => new Error('Sin conexión')));
    componente.buscarDisponibilidad();
    expect(componente.filtrado()).toBe(false);
    expect(componente.buscando()).toBe(false);
    expect(componente.errorBusqueda).toContain('No pudimos consultar');
  });
});
