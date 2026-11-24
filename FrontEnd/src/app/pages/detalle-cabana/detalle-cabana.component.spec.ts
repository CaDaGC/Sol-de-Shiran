import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { DetalleCabanaComponent } from './detalle-cabana.component';
import { AuthService } from '../../services/auth';
import { ReservasService } from '../../services/reservas.service';
import { CabanaService } from '../../services/cabana.service';

describe('Calendario con horario fijo',()=>{
  it('bloquea la reserva ajena y conserva las horas fijas sin selector',()=>{
    TestBed.configureTestingModule({imports:[DetalleCabanaComponent],providers:[provideHttpClient(),provideRouter([]),
      {provide:AuthService,useValue:{}},{provide:ReservasService,useValue:{}},{provide:CabanaService,useValue:{}}]});
    const f=TestBed.createComponent(DetalleCabanaComponent);const c=f.componentInstance;
    const consulta=vi.spyOn(c,'consultarDisponibilidad').mockImplementation(()=>{});
    c.intervalos.set([{inicio:'2099-12-01T11:00:00',fin:'2099-12-02T13:00:00'}]);
    c.seleccionarDia({fecha:'2099-12-01',numero:1,mesActual:true,pasado:false});
    expect(c.fechaIngreso()).toBe('');expect(consulta).not.toHaveBeenCalled();
    expect(c.fechaOcupada('2099-12-02')).toBe(true);
    c.seleccionarDia({fecha:'2099-12-03',numero:3,mesActual:true,pasado:false});
    expect(c.fechaIngreso()).toBe('2099-12-03');expect(c.fechaSalida()).toBe('');
    c.seleccionarDia({fecha:'2099-12-06',numero:6,mesActual:true,pasado:false});
    expect(c.fechaSalida()).toBe('2099-12-06');
    expect(c.nochesSeleccionadas()).toBe(3);
    expect(c.horaIngreso()).toBe('11:00');expect(c.horaSalida()).toBe('13:00');expect(consulta).toHaveBeenCalledOnce();
    f.destroy();
  });
  it('rechaza una salida que cruza una reserva intermedia y permite un rango libre',()=>{
    TestBed.configureTestingModule({imports:[DetalleCabanaComponent],providers:[provideHttpClient(),provideRouter([]),
      {provide:AuthService,useValue:{}},{provide:ReservasService,useValue:{}},{provide:CabanaService,useValue:{}}]});
    const f=TestBed.createComponent(DetalleCabanaComponent);const c=f.componentInstance;
    const consulta=vi.spyOn(c,'consultarDisponibilidad').mockImplementation(()=>{});
    c.intervalos.set([{inicio:'2099-11-25T11:00:00',fin:'2099-11-26T13:00:00'}]);
    const dia=(numero:number)=>({fecha:`2099-11-${numero}`,numero,mesActual:true,pasado:false});
    c.seleccionarDia(dia(22));expect(c.fechaSalida()).toBe('');
    expect(c.fechaOcupada('2099-11-28')).toBe(true);
    c.seleccionarDia(dia(28));expect(c.fechaSalida()).toBe('');expect(c.mensajeRango()).toContain('fechas ocupadas');expect(consulta).not.toHaveBeenCalled();
    expect(c.fechaOcupada('2099-11-24')).toBe(false);
    c.seleccionarDia(dia(24));expect(c.fechaSalida()).toBe('2099-11-24');expect(consulta).toHaveBeenCalledOnce();
    c.seleccionarDia(dia(28));expect(c.fechaIngreso()).toBe('2099-11-28');expect(c.fechaSalida()).toBe('');
    c.seleccionarDia(dia(27));expect(c.fechaIngreso()).toBe('2099-11-27');expect(c.fechaSalida()).toBe('');f.destroy();
  });
});
