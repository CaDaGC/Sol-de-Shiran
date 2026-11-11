import { describe, expect, it } from 'vitest';
import { rangoMetricasValido } from './metricas.component';

describe('HU19 rango de fechas', () => {
  it('acepta un día válido y hasta 367 días inclusivos', () => {
    expect(rangoMetricasValido('2024-02-29', '2024-02-29')).toBe(true);
    expect(rangoMetricasValido('2024-01-01', '2025-01-01')).toBe(true);
  });

  it('rechaza fechas inexistentes, invertidas y rangos demasiado largos', () => {
    expect(rangoMetricasValido('2026-02-29', '2026-03-01')).toBe(false);
    expect(rangoMetricasValido('2026-11-30', '2026-11-01')).toBe(false);
    expect(rangoMetricasValido('2024-01-01', '2025-01-02')).toBe(false);
  });
});
