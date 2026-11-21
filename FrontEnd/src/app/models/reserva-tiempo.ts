export interface IntervaloReserva { inicio: string; fin: string }
export function salida24Horas(fecha: string): string {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(fecha)) return '';
  const d = new Date(fecha+'T12:00:00Z');
  if (!Number.isFinite(d.getTime())) return '';
  d.setUTCDate(d.getUTCDate()+1);
  return d.toISOString().slice(0,10);
}
export function cruzaHorario(fecha: string, hora: string, intervalos: IntervaloReserva[], horaSalida = hora): boolean {
  if (!salida24Horas(fecha) || !/^([01]\d|2[0-3]):[0-5]\d$/.test(hora)) return true;
  const inicio=fecha+'T'+hora+':00', fin=salida24Horas(fecha)+'T'+horaSalida+':00';
  return intervalos.some(r => r.inicio < fin && r.fin > inicio);
}

export function cruzaRango(entrada: string, salida: string, intervalos: IntervaloReserva[]): boolean {
  if (!entrada || !salida || salida <= entrada) return true;
  const inicio = entrada + 'T11:00:00', fin = salida + 'T13:00:00';
  return intervalos.some(r => r.inicio < fin && r.fin > inicio);
}
