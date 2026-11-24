import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DisponibilidadNombreComponent } from './disponibilidad-nombre.component';

describe('Disponibilidad del nombre público', () => {
  let http: HttpTestingController;
  const esperar = () => new Promise(resolve => setTimeout(resolve, 380));

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [DisponibilidadNombreComponent], providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('no consulta nombres vacíos ni marca ocupado el nombre propio', () => {
    const f = TestBed.createComponent(DisponibilidadNombreComponent);
    f.componentRef.setInput('nombreActual', 'Carlos');
    f.componentRef.setInput('nombre', ' carlos '); f.detectChanges();
    expect(f.componentInstance.estado()).toBe('disponible');
    http.expectNone(r => r.url.includes('nombre-disponible'));
    f.componentRef.setInput('nombre', ''); f.detectChanges();
    expect(f.componentInstance.estado()).toBe('inicial'); f.destroy();
  });

  it('muestra alternativas y permite seleccionar una sin enviar el formulario', async () => {
    const f = TestBed.createComponent(DisponibilidadNombreComponent);
    const elegido = vi.fn(); const bloqueado = vi.fn();
    f.componentInstance.seleccionar.subscribe(elegido); f.componentInstance.bloqueado.subscribe(bloqueado);
    f.componentRef.setInput('nombre', 'Carlos'); f.detectChanges(); await esperar();
    const req = http.expectOne(r => r.url.endsWith('/usuarios/nombre-disponible'));
    expect(req.request.params.get('nombre')).toBe('Carlos');
    req.flush({ nombre: 'Carlos', disponible: false, sugerencias: ['carlos_1', 'carlos_2', 'carlos_3', 'carlos_4'] }); f.detectChanges();
    expect(f.nativeElement.textContent).toContain('ya está ocupado');expect(f.nativeElement.querySelectorAll('.sugerencias button')).toHaveLength(2);
    const boton = f.nativeElement.querySelector('button'); expect(boton.type).toBe('button'); boton.click();
    expect(elegido).toHaveBeenCalledWith('carlos_1'); expect(bloqueado).toHaveBeenLastCalledWith(true); f.destroy();
  });

  it('cancela la petición anterior para no mostrar sugerencias de otro texto', async () => {
    const f = TestBed.createComponent(DisponibilidadNombreComponent);
    f.componentRef.setInput('nombre', 'anterior'); f.detectChanges(); await esperar();
    const anterior = http.expectOne(r => r.params.get('nombre') === 'anterior');
    f.componentRef.setInput('nombre', 'nuevo'); f.detectChanges(); expect(anterior.cancelled).toBe(true); await esperar();
    http.expectOne(r => r.params.get('nombre') === 'nuevo').flush({ nombre: 'nuevo', disponible: true, sugerencias: [] });
    expect(f.componentInstance.estado()).toBe('disponible'); f.destroy();
  });

  it('un fallo de red informa sin bloquear indefinidamente el guardado', async () => {
    const f = TestBed.createComponent(DisponibilidadNombreComponent); const bloqueado = vi.fn();
    f.componentInstance.bloqueado.subscribe(bloqueado);
    f.componentRef.setInput('nombre', 'nuevo'); f.detectChanges(); await esperar();
    http.expectOne(r => r.url.endsWith('/usuarios/nombre-disponible')).flush({}, { status: 503, statusText: 'Unavailable' });
    expect(f.componentInstance.estado()).toBe('error'); expect(bloqueado).toHaveBeenLastCalledWith(false); f.destroy();
  });
});
