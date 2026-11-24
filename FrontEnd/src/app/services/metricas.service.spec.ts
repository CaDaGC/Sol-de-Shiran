import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { MetricasService } from './metricas.service';

describe('HU19 consultas del dashboard', () => {
  it('consulta ambos microservicios con el mismo rango y carga nombres de cabañas', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const servicio = TestBed.inject(MetricasService);
    const http = TestBed.inject(HttpTestingController);
    const resultado = firstValueFrom(servicio.consultar('2026-11-01', '2026-11-30'));

    const pagos = http.expectOne(r => r.url.endsWith('/pagos/metricas/ingresos'));
    const reservas = http.expectOne(r => r.url.endsWith('/reservas/metricas/frecuencia'));
    const cabanas = http.expectOne(r => r.url.endsWith('/cabanas/gestion'));
    expect(pagos.request.params.get('desde')).toBe('2026-11-01');
    expect(reservas.request.params.get('hasta')).toBe('2026-11-30');
    pagos.flush({ desde: '2026-11-01', hasta: '2026-11-30', totalIngresos: 150, numeroPagos: 2, ingresosPorDia: [] });
    reservas.flush({ desde: '2026-11-01', hasta: '2026-11-30', totalReservas: 1, ranking: [{ idCabana: 1, reservas: 1 }] });
    cabanas.flush([{ idCabana: 1, nombre: 'Cabaña Jardín' }]);

    const datos = await resultado;
    expect(datos.ingresos.totalIngresos).toBe(150);
    expect(datos.frecuencia.ranking[0].idCabana).toBe(1);
    expect(datos.cabanas[0].nombre).toBe('Cabaña Jardín');
    http.verify();
  });
});
