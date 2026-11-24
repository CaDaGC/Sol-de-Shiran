import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
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
  it('separa usuario y correo y filtra por rol sin mezclar cuentas',()=>{
    const f=crear();const c=f.componentInstance;c.abrirCuentas();
    c.usuarios.set([{...perfil,id:2,rol:'CLIENTE',nombreUsuario:'cliente'},{...perfil,id:3,rol:'ADMIN',nombreUsuario:'admin2'}]);f.detectChanges();
    expect(f.nativeElement.querySelector('nav[aria-label="Secciones de gestión"]')).toBeNull();
    expect(f.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1);
    expect(c.usuariosFiltrados()[0].rol).toBe('CLIENTE');
    c.filtroRol.set('ADMIN');f.detectChanges();
    expect(c.usuariosFiltrados()[0].nombreUsuario).toBe('admin2');
    const cabeceras=Array.from(f.nativeElement.querySelectorAll('th') as NodeListOf<HTMLElement>).map(e=>e.textContent?.trim());
    expect(cabeceras).toContain('Usuario');expect(cabeceras).toContain('Correo');f.destroy();
  });
  it('actualiza la sección al navegar desde el sidebar sin recargar la página',async()=>{
    const f=crear();await TestBed.inject(Router).navigateByUrl('/?seccion=cuentas');await f.whenStable();
    expect(f.componentInstance.tab).toBe('cuentas');expect(f.nativeElement.querySelector('section h2')?.textContent).toBe('Usuarios');
    await TestBed.inject(Router).navigateByUrl('/?seccion=bloqueos');await f.whenStable();
    expect(f.nativeElement.textContent).toContain('Bloquear disponibilidad');f.destroy();
  });
  it('oculta persona para ADMIN y no envía dueño ni asignaciones',async()=>{
    const f=crear();const c=f.componentInstance;c.abrirCuentas(); c.crearUsuario(); c.usuario.rol = 'ADMIN';f.detectChanges();await f.whenStable();
    expect(f.nativeElement.querySelector('[name="unumeroDocumento"]')).toBeNull();
    c.usuario.nombreUsuario='nuevoAdmin';c.usuario.email='nuevo@example.com';c.usuario.password='Clave1234';c.guardarUsuario();http.expectNone(r=>r.method==='POST');c.confirmarAccion();
    const req=http.expectOne(r=>r.method==='POST');expect(req.request.body).toEqual({nombreUsuario:'nuevoAdmin',email:'nuevo@example.com',rol:'ADMIN',password:'Clave1234'});
    req.flush({detail:'Prueba'},{status:409,statusText:'Conflict'});f.destroy();
  });
  it('exige persona para cada rol no administrativo',async()=>{
    const f=crear();const c=f.componentInstance;
    for(const seccion of c.seccionesCuentas.slice(0,3)){
      c.abrirCuentas(); c.crearUsuario(); c.usuario.rol = seccion.rol;f.detectChanges();await f.whenStable();
      expect(f.nativeElement.querySelector('[name="unumeroDocumento"]').required).toBe(true);
      c.usuario.nombreUsuario='cliente';c.usuario.email='c@example.com';c.usuario.password='Clave1234';c.guardarUsuario();http.expectNone(r=>r.method==='POST');
      c.usuario.persona={...persona};c.guardarUsuario();c.confirmarAccion();const req=http.expectOne(r=>r.method==='POST');expect(req.request.body.persona).toEqual(persona);expect(req.request.body.rol).toBe(seccion.rol);
      req.flush({detail:'Prueba'},{status:409,statusText:'Conflict'});
    }f.destroy();
  });
  it('edita alias y contacto conservando documento y datos civiles',()=>{
    const f=crear();const c=f.componentInstance;c.perfil.set({...perfil,persona:{...persona,telefono:'912345678',direccion:'Nueva dirección'},nombreUsuario:'alias'});c.guardarPerfil();c.confirmarAccion();
    const req=http.expectOne(r=>r.method==='PUT');expect(req.request.body.nombreUsuario).toBe('alias');expect(req.request.body.persona.numeroDocumento).toBe('12345678');expect(req.request.body.persona.telefono).toBe('912345678');expect(req.request.body.rol).toBeUndefined();req.flush({...perfil,nombreUsuario:'alias',persona});expect(auth.actualizarPerfil).toHaveBeenCalled();f.destroy();
  });
  it('sube la imagen mediante MultipartFile con el campo file',()=>{
    const f=crear();const c=f.componentInstance;const foto=new File(['foto'],'foto.png',{type:'image/png'});c.archivoFotoPerfil=foto;c.guardarFotoPerfil();http.expectNone(r=>r.method==='POST');c.confirmarAccion();
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
    const f=crear();const c=f.componentInstance;c.abrirCuentas();c.usuarios.set([{...perfil,id:2,rol:'CLIENTE',nombreUsuario:'Ana',email:'ana@example.com'},{...perfil,id:3,rol:'CLIENTE',nombreUsuario:'Luis',email:'luis@correo.pe'}]);
    c.buscarUsuario.set('ANA');f.detectChanges();expect(f.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1);expect(c.usuariosFiltrados()[0].nombreUsuario).toBe('Ana');
    c.buscarUsuario.set('@correo.pe');expect(c.usuariosFiltrados()[0].nombreUsuario).toBe('Luis');
    expect(f.nativeElement.textContent).not.toContain('Cabañas del dueño');f.destroy();
  });
  it('mueve crear y editar cabañas al mismo modal y conserva la edición si falla el guardado',()=>{
    const f=crear();const c=f.componentInstance;c.crearCabana();expect(c.editor()).toBe('cabana');expect(c.cabana.idCabana).toBeNull();
    c.cerrarEditor();c.editarCabana({...c.nuevaCabana(),idCabana:7,nombre:'Luna',imagenesSecundarias:['/luna.jpg']});
    expect(c.cabana.nombre).toBe('Luna');expect(c.imagenes).toBe('/luna.jpg');
    c.guardarCabana();c.confirmarAccion();http.expectOne(r=>r.method==='PUT' && r.url.endsWith('/cabanas/7')).flush({detail:'No disponible'},{status:503,statusText:'Unavailable'});
    expect(c.editor()).toBe('cabana');expect(c.cabana.nombre).toBe('Luna');expect(c.error()).toBe('No disponible');f.destroy();
  });
  it('confirma desactivación en un modal y cambia el icono al reactivar',()=>{
    const f=crear();const c=f.componentInstance;c.abrirCuentas();const cuenta={...perfil,id:2,rol:'CLIENTE'};c.usuarios.set([cuenta]);
    c.solicitarEstado(cuenta);f.detectChanges();expect(f.nativeElement.querySelector('#estado-usuario-title').textContent).toContain('Desactivar');
    http.expectNone(r=>r.method==='PUT');c.confirmarEstado();
    const req=http.expectOne(r=>r.method==='PUT' && r.url.endsWith('/usuarios/2/estado'));expect(req.request.body).toEqual({activo:false});req.flush({...cuenta,activo:false});f.detectChanges();
    expect(c.usuarios()[0].activo).toBe(false);expect(c.mensaje()).toBe('Cuenta desactivada.');
    expect(f.nativeElement.querySelector('[title="Reactivar cuenta"]').classList.contains('btn-outline-success')).toBe(true);
    c.solicitarEstado({...cuenta,activo:false});c.confirmarEstado();
    const reactivar=http.expectOne(r=>r.method==='PUT');expect(reactivar.request.body).toEqual({activo:true});reactivar.flush(cuenta);expect(c.mensaje()).toBe('Cuenta reactivada.');f.destroy();
  });
  it('cancelar no cambia datos y un fallo conserva la confirmación para reintentar',()=>{
    const f=crear();const c=f.componentInstance;const cuenta={...perfil,id:2};
    c.solicitarEstado(cuenta);c.cerrarEstado();http.expectNone(r=>r.method==='PUT');
    c.solicitarEstado(cuenta);c.confirmarEstado();http.expectOne(r=>r.method==='PUT').flush({detail:'No disponible'},{status:503,statusText:'Unavailable'});
    expect(c.errorEstado()).toBe('No disponible');expect(c.cuentaEstado()?.id).toBe(2);expect(c.ocupado()).toBe(false);c.cerrarEstado();f.destroy();
  });
  it('muestra datos civiles solo en lectura y admite cuentas sin persona',()=>{
    const f=crear();const c=f.componentInstance;c.verUsuario({...perfil,id:2,persona});f.detectChanges();
    const modal=f.nativeElement.querySelector('dialog[aria-labelledby="detalle-usuario-title"]');expect(modal.open).toBe(true);expect(modal.textContent).toContain('12345678');expect(modal.querySelector('input,select,textarea,form')).toBeNull();expect(modal.textContent).not.toContain('ID de usuario');expect(modal.textContent).not.toContain('ID de persona');expect(modal.querySelectorAll('.detalle-grupo')).toHaveLength(2);
    c.cerrarDetalle();c.verUsuario(perfil);f.detectChanges();expect(modal.textContent).toContain('no tiene una persona asociada');c.cerrarDetalle();
    c.solicitarEstado(perfil);expect(c.cuentaEstado()).toBeNull();f.destroy();
  });
  it('solicita confirmación antes de editar y cancelar conserva el formulario',async()=>{
    const f=crear();const c=f.componentInstance;c.editarUsuario({...perfil,id:2,nombreUsuario:'anterior'});
    c.usuario.nombreUsuario='actualizado';c.guardarUsuario();f.detectChanges();
    expect(f.nativeElement.querySelector('dialog[aria-labelledby="confirmar-edicion-title"]').open).toBe(true);
    http.expectNone(r=>r.method==='PUT');c.cerrarConfirmacionEdicion();expect(c.usuario.nombreUsuario).toBe('actualizado');expect(c.editor()).toBe('usuario');
    await new Promise(resolve=>setTimeout(resolve,400));http.expectOne(r=>r.url.endsWith('/usuarios/nombre-disponible')).flush({nombre:'actualizado',disponible:true,sugerencias:[]});
    c.guardarUsuario();c.confirmarEdicion();const req=http.expectOne(r=>r.method==='PUT');expect(req.request.body.nombreUsuario).toBe('actualizado');req.flush({...perfil,id:2,nombreUsuario:'actualizado'});
    http.expectOne(r=>r.url.endsWith('/cabanas/gestion')).flush([]);http.expectOne(r=>r.url.endsWith('/usuarios')).flush([]);
    expect(c.edicionPendiente()).toBeNull();expect(c.editor()).toBeNull();f.destroy();
  });
  it('conserva la edición y permite cancelar si el servidor rechaza el guardado',()=>{
    const f=crear();const c=f.componentInstance;c.editarUsuario({...perfil,id:2});c.guardarUsuario();c.confirmarEdicion();
    http.expectOne(r=>r.method==='PUT').flush({detail:'El nombre de usuario ya existe'},{status:409,statusText:'Conflict'});
    expect(c.errorEdicion()).toContain('nombre de usuario');expect(c.editor()).toBe('usuario');expect(c.ocupado()).toBe(false);c.cerrarConfirmacionEdicion();f.destroy();
  });
  it('cancelar guardados y cambios de estado de cabaña no envía peticiones',()=>{
    const f=crear();const c=f.componentInstance;const cabana={...c.nuevaCabana(),idCabana:5,nombre:'Luna'};
    c.desactivarCabana(cabana);http.expectNone(r=>r.method==='DELETE');c.cancelarAccion();
    c.activar(cabana);http.expectNone(r=>r.method==='PUT');c.confirmarAccion();
    http.expectOne(r=>r.method==='PUT'&&r.url.endsWith('/cabanas/5/activar')).flush({});
    http.expectOne(r=>r.url.endsWith('/cabanas/gestion')).flush([]);http.expectOne(r=>r.url.endsWith('/usuarios')).flush([]);
    c.desactivarCabana(cabana);c.confirmarAccion();http.expectOne(r=>r.method==='DELETE').flush({});
    http.expectOne(r=>r.url.endsWith('/cabanas/gestion')).flush([]);http.expectOne(r=>r.url.endsWith('/usuarios')).flush([]);f.destroy();
  });
  it('explica rangos inválidos y confirma bloquear y retirar fechas',()=>{
    const f=crear();const c=f.componentInstance;c.bloqueo={idCabana:5,desde:'2099-12-10',hasta:'2099-12-09',motivo:'Mantenimiento'};
    c.bloquear();expect(c.confirmacion()).toBeNull();expect(c.notificaciones.aviso()?.mensaje).toContain('fecha final');http.expectNone(r=>r.method==='POST');
    c.bloqueo.hasta='2099-12-12';c.bloquear();c.cancelarAccion();http.expectNone(r=>r.method==='POST');
    c.bloquear();c.confirmarAccion();http.expectOne(r=>r.method==='POST').flush({});http.expectOne(r=>r.url.endsWith('/reservas/bloqueos')).flush([]);
    c.desbloquear({id:7,desde:'2099-12-10',hasta:'2099-12-12'});c.confirmarAccion();http.expectOne(r=>r.method==='DELETE'&&r.url.endsWith('/reservas/bloqueos/7')).flush(null);http.expectOne(r=>r.url.endsWith('/reservas/bloqueos')).flush([]);f.destroy();
  });
  it('confirma cambios de contraseña y muestra el motivo real del servidor',()=>{
    const f=crear();const c=f.componentInstance;c.passwordActual='anterior';c.passwordNueva='NuevaClave123';c.cambiarPassword();
    http.expectNone(r=>r.method==='PUT');c.confirmarAccion();
    http.expectOne(r=>r.url.endsWith('/password')).flush({detail:'La contraseña actual no es correcta.'},{status:400,statusText:'Bad Request'});
    expect(c.notificaciones.aviso()?.tipo).toBe('error');expect(c.notificaciones.aviso()?.mensaje).toContain('contraseña actual');f.destroy();
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
