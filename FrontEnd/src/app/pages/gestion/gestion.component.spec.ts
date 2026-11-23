import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { GestionComponent } from './gestion.component';
import { AuthService } from '../../services/auth';
const persona={id:9,nombres:'Carlos',apellidos:'Gonzales',tipoDocumento:'DNI',numeroDocumento:'12345678',telefono:'987654321',direccion:'Calle Sol 123'};
const perfil={id:1,nombre:'admin',nombreUsuario:'admin',email:'admin@example.com',rol:'ADMIN',activo:true,fotoPerfil:null,persona:null};
describe('Usuarios y perfil con datos civiles',()=>{
  const originales = ['showModal', 'close'].map(k => Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, k));
  beforeAll(() => {
    Object.defineProperty(HTMLDialogElement.prototype, 'showModal', {configurable:true,value(this:HTMLDialogElement){this.open=true;}});
    Object.defineProperty(HTMLDialogElement.prototype, 'close', {configurable:true,value(this:HTMLDialogElement){this.open=false;}});
  });
  afterAll(() => ['showModal','close'].forEach((k,i) => { if(originales[i]) Object.defineProperty(HTMLDialogElement.prototype,k,originales[i]!); else Reflect.deleteProperty(HTMLDialogElement.prototype,k); }));
  let http:HttpTestingController;
  const auth={estaAutenticado:()=>true,actualizarPerfil:vi.fn(),cargarFotoPerfil:vi.fn(),fotoPerfil:signal<string|null>(null)};
  beforeEach(()=>{TestBed.configureTestingModule({imports:[GestionComponent],providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:AuthService,useValue:auth}]});http=TestBed.inject(HttpTestingController);auth.actualizarPerfil.mockClear();});
  afterEach(()=>http.verify());
  function crear(){const f=TestBed.createComponent(GestionComponent);f.detectChanges();http.expectOne(r=>r.url.endsWith('/usuarios/me')).flush(perfil);http.expectOne(r=>r.url.endsWith('/cabanas/gestion')).flush([]);http.expectOne(r=>r.url.endsWith('/usuarios')).flush([]);return f;}
  it('presenta una sección Usuarios con cuentas de todos los roles',()=>{
    const f=crear();const c=f.componentInstance;c.abrirCuentas();
    c.usuarios.set([{...perfil,id:2,rol:'CLIENTE',nombreUsuario:'cliente'},{...perfil,id:3,rol:'ADMIN',nombreUsuario:'admin2'}]);f.detectChanges();
    const botones=Array.from(f.nativeElement.querySelectorAll('nav button') as NodeListOf<HTMLButtonElement>).map(b=>b.textContent?.trim());
    expect(botones).toContain('Usuarios');expect(botones).not.toContain('Clientes');expect(botones).not.toContain('Administradores');
    expect(f.nativeElement.querySelectorAll('tbody tr')).toHaveLength(2);f.destroy();
  });
  it('oculta persona para ADMIN y no envía dueño ni asignaciones',async()=>{
    const f=crear();const c=f.componentInstance;c.abrirCuentas(); c.crearUsuario(); c.usuario.rol = 'ADMIN';f.detectChanges();await f.whenStable();
    expect(f.nativeElement.querySelector('[name="unumeroDocumento"]')).toBeNull();
    c.usuario.nombreUsuario='nuevoAdmin';c.usuario.email='nuevo@example.com';c.usuario.password='Clave1234';c.guardarUsuario();
    const req=http.expectOne(r=>r.method==='POST');expect(req.request.body).toEqual({nombreUsuario:'nuevoAdmin',email:'nuevo@example.com',rol:'ADMIN',password:'Clave1234'});
    req.flush({detail:'Prueba'},{status:409,statusText:'Conflict'});f.destroy();
  });
  it('exige persona para cada rol no administrativo',async()=>{
    const f=crear();const c=f.componentInstance;
    for(const seccion of c.seccionesCuentas.slice(0,3)){
      c.abrirCuentas(); c.crearUsuario(); c.usuario.rol = seccion.rol;f.detectChanges();await f.whenStable();
      expect(f.nativeElement.querySelector('[name="unumeroDocumento"]').required).toBe(true);
      c.usuario.nombreUsuario='cliente';c.usuario.email='c@example.com';c.usuario.password='Clave1234';c.guardarUsuario();http.expectNone(r=>r.method==='POST');
      c.usuario.persona={...persona};c.guardarUsuario();const req=http.expectOne(r=>r.method==='POST');expect(req.request.body.persona).toEqual(persona);expect(req.request.body.rol).toBe(seccion.rol);
      req.flush({detail:'Prueba'},{status:409,statusText:'Conflict'});
    }f.destroy();
  });
  it('edita alias y contacto conservando documento y datos civiles',()=>{
    const f=crear();const c=f.componentInstance;c.perfil.set({...perfil,persona:{...persona,telefono:'912345678',direccion:'Nueva dirección'},nombreUsuario:'alias'});c.guardarPerfil();
    const req=http.expectOne(r=>r.method==='PUT');expect(req.request.body.nombreUsuario).toBe('alias');expect(req.request.body.persona.numeroDocumento).toBe('12345678');expect(req.request.body.persona.telefono).toBe('912345678');expect(req.request.body.rol).toBeUndefined();req.flush({...perfil,nombreUsuario:'alias',persona});expect(auth.actualizarPerfil).toHaveBeenCalled();f.destroy();
  });
  it('sube la imagen mediante MultipartFile con el campo file',()=>{
    const f=crear();const c=f.componentInstance;const foto=new File(['foto'],'foto.png',{type:'image/png'});c.archivoFotoPerfil=foto;c.guardarFotoPerfil();
    const req=http.expectOne(r=>r.method==='POST' && r.url.endsWith('/usuarios/1/foto'));
    expect(req.request.body instanceof FormData).toBe(true);expect(req.request.body.get('file')).toBe(foto);expect(req.request.body.has('archivo')).toBe(false);
    req.flush({...perfil,fotoPerfil:'/api/v1/usuarios/fotos/foto.png'});expect(auth.actualizarPerfil).toHaveBeenLastCalledWith(expect.objectContaining({fotoPerfil:'/api/v1/usuarios/fotos/foto.png'}));f.destroy();
  });
  it('abre el modal vacío para crear y con copia de los datos para editar',()=>{
    const f=crear();const c=f.componentInstance;c.abrirCuentas();f.detectChanges();
    expect(c.editor()).toBeNull();expect(f.nativeElement.querySelector('dialog').open).toBe(false);
    c.editarUsuario({...perfil,id:2,nombreUsuario:'alias',persona:{...persona}});f.detectChanges();
    expect(f.nativeElement.querySelector('dialog').open).toBe(true);expect(c.usuario.nombreUsuario).toBe('alias');
    c.usuario.persona.telefono='otro';expect(persona.telefono).toBe('987654321');
    c.cerrarEditor();c.crearUsuario();f.detectChanges();expect(c.usuario.id).toBeNull();expect(c.usuario.nombreUsuario).toBe('');expect(c.usuario.persona.telefono).toBe('');
    c.cerrarEditor();f.destroy();
  });
  it('busca por alias o correo sin distinguir mayúsculas y retira la columna de cabañas',()=>{
    const f=crear();const c=f.componentInstance;c.abrirCuentas();c.usuarios.set([{...perfil,id:2,nombreUsuario:'Ana',email:'ana@example.com'},{...perfil,id:3,nombreUsuario:'Luis',email:'luis@correo.pe'}]);
    c.buscarUsuario.set('ANA');f.detectChanges();expect(f.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1);expect(c.usuariosFiltrados()[0].nombreUsuario).toBe('Ana');
    c.buscarUsuario.set('@correo.pe');expect(c.usuariosFiltrados()[0].nombreUsuario).toBe('Luis');
    expect(f.nativeElement.textContent).not.toContain('Cabañas del dueño');f.destroy();
  });
  it('mueve crear y editar cabañas al mismo modal y conserva la edición si falla el guardado',()=>{
    const f=crear();const c=f.componentInstance;c.crearCabana();expect(c.editor()).toBe('cabana');expect(c.cabana.idCabana).toBeNull();
    c.cerrarEditor();c.editarCabana({...c.nuevaCabana(),idCabana:7,nombre:'Luna',imagenesSecundarias:['/luna.jpg']});
    expect(c.cabana.nombre).toBe('Luna');expect(c.imagenes).toBe('/luna.jpg');
    c.guardarCabana();http.expectOne(r=>r.method==='PUT' && r.url.endsWith('/cabanas/7')).flush({detail:'No disponible'},{status:503,statusText:'Unavailable'});
    expect(c.editor()).toBe('cabana');expect(c.cabana.nombre).toBe('Luna');expect(c.error()).toBe('No disponible');f.destroy();
  });
  it('desactiva sin anunciar borrado físico y refresca el estado en la lista',()=>{
    const f=crear();const c=f.componentInstance;const confirmar=vi.spyOn(window,'confirm').mockReturnValue(true);
    c.eliminarUsuario({...perfil,id:2,rol:'CLIENTE'});expect(confirmar).toHaveBeenCalledWith(expect.stringContaining('Desactivar'));
    http.expectOne(r=>r.method==='DELETE' && r.url.endsWith('/usuarios/2')).flush(null);
    http.expectOne(r=>r.url.endsWith('/cabanas/gestion')).flush([]);http.expectOne(r=>r.url.endsWith('/usuarios')).flush([{...perfil,id:2,activo:false}]);
    expect(c.usuarios()[0].activo).toBe(false);expect(c.mensaje()).toBe('Cuenta desactivada.');confirmar.mockRestore();f.destroy();
  });
  it('desplaza foto y seguridad dentro de la cuenta sin seguir el enlace al inicio', () => {
    const f = crear();
    f.componentInstance.tab = 'perfil';
    f.detectChanges();
    for (const id of ['foto-perfil', 'seguridad-cuenta']) {
      const destino = f.nativeElement.querySelector('#' + id) as HTMLElement;
      const desplazar = vi.fn();
      destino.scrollIntoView = desplazar;
      const evento = new MouseEvent('click', { cancelable: true });
      f.componentInstance.irASeccion(id as 'foto-perfil' | 'seguridad-cuenta', evento);
      expect(evento.defaultPrevented).toBe(true);
      expect(desplazar).toHaveBeenCalledWith({ block: 'start' });
      expect(f.componentInstance.seccionCuenta()).toBe(id);
    }
    f.destroy();
  });

});
