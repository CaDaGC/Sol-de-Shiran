import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RegistroComponent } from './registro.component';

const persona = { nombres:'Carlos',apellidos:'Gonzales',tipoDocumento:'DNI',numeroDocumento:'12345678',telefono:'987654321',direccion:'Calle Sol 123' };
describe('Registro público con persona', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    Object.defineProperty(HTMLDialogElement.prototype, 'showModal', { configurable: true, value(this: HTMLDialogElement) { this.open = true; } });
    Object.defineProperty(HTMLDialogElement.prototype, 'close', { configurable: true, value(this: HTMLDialogElement) { this.open = false; } });
    TestBed.configureTestingModule({imports:[RegistroComponent],
    providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([])]}); http=TestBed.inject(HttpTestingController); });
  afterEach(()=>http.verify());
  it('exige todos los datos y envía persona sin permitir elegir un rol', async () => {
    const fixture=TestBed.createComponent(RegistroComponent); fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges();
    const button=fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(button.disabled).toBe(false);
    button.click(); http.expectNone(r => r.method === 'POST');
    expect(fixture.nativeElement.querySelector('.campo-invalido')).toBeTruthy();
    const campos={nombreUsuario:'carlos',email:'carlos@example.com',password:'Clave1234',...persona};
    for(const [name,value] of Object.entries(campos)) {
      const input=fixture.nativeElement.querySelector(`[name="${name}"]`) as HTMLInputElement;
      input.value=value; input.dispatchEvent(new Event(name === 'tipoDocumento' ? 'change' : 'input'));
    }
    fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges();
    await new Promise(resolve=>setTimeout(resolve,400));
    http.expectOne(r=>r.url.endsWith('/usuarios/nombre-disponible')).flush({nombre:'carlos',disponible:true,sugerencias:[]});fixture.detectChanges();
    expect(button.disabled).toBe(false); button.click();
    http.expectNone(r => r.method === 'POST');
    document.querySelector<HTMLButtonElement>('.confirmacion-escritura .btn-success')!.click();
    const req=http.expectOne(r=>r.method==='POST' && r.url.endsWith('/usuarios/register'));
    expect(req.request.body).toEqual({nombreUsuario:'carlos',email:'carlos@example.com',password:'Clave1234',persona});
    req.flush({id:1}); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Tu cuenta se creó correctamente'); fixture.destroy();
  });
  it('muestra el error de documento duplicado sin dar por creada la cuenta', () => {
    const fixture=TestBed.createComponent(RegistroComponent); const c=fixture.componentInstance;
    c.nombreUsuario='carlos';c.email='carlos@example.com';c.password='Clave1234';c.persona={...persona};
    c.registrar({invalid:false} as any);
    http.expectOne(r=>r.method==='POST').flush({detail:'El documento ya está registrado.'},{status:409,statusText:'Conflict'});
    expect(c.completado()).toBe(false);expect(c.error()).toContain('documento');fixture.destroy();
  });
  it('explica que una contraseña de seis caracteres bloquea el envío y habilita al corregirla', async () => {
    const f=TestBed.createComponent(RegistroComponent);f.detectChanges();await f.whenStable();
    const campos={nombreUsuario:'carlos10',email:'carlosPrueba@gmail.com',password:'123456',...persona};
    for(const [name,value] of Object.entries(campos)) {
      const input=f.nativeElement.querySelector(`[name="${name}"]`) as HTMLInputElement;
      input.value=value;input.dispatchEvent(new Event(name==='tipoDocumento'?'change':'input'));
    }
    await f.whenStable();f.detectChanges();
    const boton=f.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(boton.disabled).toBe(true);expect(f.nativeElement.textContent).toContain('Contraseña: usa al menos 8 caracteres.');
    http.expectNone(r=>r.method==='POST');
    const password=f.nativeElement.querySelector('[name="password"]') as HTMLInputElement;
    password.value='12345678';password.dispatchEvent(new Event('input'));await f.whenStable();f.detectChanges();
    await new Promise(resolve=>setTimeout(resolve,400));
    http.expectOne(r=>r.url.endsWith('/usuarios/nombre-disponible')).flush({nombre:'carlos10',disponible:true,sugerencias:[]});f.detectChanges();
    expect(boton.disabled).toBe(false);expect(f.nativeElement.textContent).not.toContain('Contraseña: usa al menos 8 caracteres.');
    f.destroy();
  });
});
