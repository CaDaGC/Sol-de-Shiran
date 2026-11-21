import {salida24Horas, cruzaHorario} from './reserva-tiempo';
describe('Estancia fija de 24 horas',()=> {
 it('calcula la salida en cambios de mes y año',()=> {
  expect(salida24Horas('2026-12-31')).toBe('2027-01-01');
  expect(salida24Horas('2028-02-28')).toBe('2028-02-29');
 });
 it('bloquea cruces de otro cliente y permite el límite exacto',()=> {
  const r=[{inicio:'2026-12-01T14:00:00',fin:'2026-12-02T14:00:00'}];
  expect(cruzaHorario('2026-12-02','13:59',r)).toBe(true);
  expect(cruzaHorario('2026-12-02','14:00',r)).toBe(false);
  expect(cruzaHorario('2026-11-30','14:01',r)).toBe(true);
 });
 it('no admite una hora incompleta',()=>expect(cruzaHorario('2026-12-01','25:00',[])).toBe(true));
 it('respeta ingreso fijo 11:00 y salida 13:00 sin permitir el cruce entre clientes',()=>{
   const ocupada=[{inicio:'2026-12-01T11:00:00',fin:'2026-12-02T13:00:00'}];
   expect(cruzaHorario('2026-12-01','11:00',ocupada,'13:00')).toBe(true);
   expect(cruzaHorario('2026-12-02','11:00',ocupada,'13:00')).toBe(true);
   expect(cruzaHorario('2026-12-03','11:00',ocupada,'13:00')).toBe(false);
 });
});
