import { FormularioDirective } from '../../shared/formulario/formulario.directive';
import { NotificacionesService } from '../../services/notificaciones.service';
import { MostrarContrasenaDirective } from '../../shared/mostrar-contrasena/mostrar-contrasena.directive';
import { CommonModule } from '@angular/common';
import { Component, computed, ElementRef, viewChild, ChangeDetectorRef, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DestroyRef } from '@angular/core';
import { PublicNavbarComponent } from '../../shared/public-navbar/public-navbar.component';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth';
import { API_CONFIG } from '../../core/api.config';
import { imagenUrl } from '../../core/imagen-url';
import { DisponibilidadNombreComponent } from '../../shared/disponibilidad-nombre/disponibilidad-nombre.component';
import { Cuenta, Persona, nuevaPersona } from '../../models/usuario.model';

type RolCuenta = 'CLIENTE' | 'TRABAJADOR' | 'DUENO' | 'ADMIN' | 'RESTAURANTE';

interface FormularioCuenta {
  id: number | null;
  nombreUsuario: string;
  email: string;
  rol: string;
  password: string;
  persona: Persona;
}

@Component({
  selector: 'app-gestion',
  standalone: true,
  imports: [FormularioDirective, MostrarContrasenaDirective, CommonModule, FormsModule, RouterLink, PublicNavbarComponent, DisponibilidadNombreComponent],
  templateUrl: './gestion.component.html',
  styleUrl: './gestion.component.css',
})
export class GestionComponent implements OnInit {
  readonly notificaciones = inject(NotificacionesService);
  readonly confirmacion = signal<{ titulo: string; mensaje: string } | null>(null);
  private readonly accionDialog = viewChild.required<ElementRef<HTMLDialogElement>>('accionDialog');
  private accionPendiente: (() => void) | null = null;
  pedirConfirmacion(titulo: string, mensaje: string, accion: () => void): void {
    if (this.ocupado() || this.guardandoFotoPerfil() || this.subiendoFotos()) return;
    this.confirmacion.set({ titulo, mensaje });
    this.accionPendiente = accion;
    this.cambios.detectChanges();
    if (!this.accionDialog().nativeElement.open) this.accionDialog().nativeElement.showModal();
  }
  cancelarAccion(): void {
    this.accionDialog().nativeElement.close();
    this.confirmacion.set(null);
    this.accionPendiente = null;
  }
  confirmarAccion(): void {
    const accion = this.accionPendiente;
    if (!accion || this.ocupado() || this.guardandoFotoPerfil()) return;
    this.cancelarAccion();
    accion();
  }
  limpiarFormularioCabana(): void {
    this.pedirConfirmacion('Limpiar formulario', '¿Estás seguro de descartar los datos de este formulario?', () => this.nueva());
  }
  quitarImagenPrincipal(): void {
    this.pedirConfirmacion('Quitar imagen principal', '¿Estás seguro de quitar la imagen principal? El cambio se aplicará al guardar la cabaña.', () => { this.cabana.imagenPrincipal = '/images/cabana-placeholder.svg'; });
  }
  readonly auth = inject(AuthService);
  readonly seccionCuenta = signal('datos-personales');
  private readonly elemento = inject<ElementRef<HTMLElement>>(ElementRef);

  irASeccion(seccion: 'datos-personales' | 'foto-perfil' | 'seguridad-cuenta', evento: Event): void {
    evento.preventDefault();
    const destino = this.elemento.nativeElement.querySelector<HTMLElement>('#' + seccion);
    if (!destino) return;
    this.seccionCuenta.set(seccion);
    destino.scrollIntoView({ block: 'start' });
  }

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  readonly url = API_CONFIG.baseUrl;
  readonly imagenUrl = imagenUrl;
  private readonly editorDialog = viewChild.required<ElementRef<HTMLDialogElement>>('editorDialog');
  private readonly cambios = inject(ChangeDetectorRef);
  readonly nombreUsuarioOriginal = signal('');
  readonly nombrePerfilOriginal = signal('');
  readonly nombreUsuarioBloqueado = signal(false);
  readonly nombrePerfilBloqueado = signal(false);
  readonly revisionNombreUsuario = signal(0);
  readonly revisionNombrePerfil = signal(0);
  readonly edicionPendiente = signal<{ id: number; datos: { nombreUsuario: string; email: string; rol: string; persona?: Persona } } | null>(null);
  readonly errorEdicion = signal('');
  private readonly edicionDialog = viewChild.required<ElementRef<HTMLDialogElement>>('edicionDialog');
  readonly editor = signal<'usuario' | 'cabana' | null>(null);
  readonly filtroRol = signal<RolCuenta>('CLIENTE');
  readonly detalleUsuario = signal<Cuenta | null>(null);
  readonly cuentaEstado = signal<Cuenta | null>(null);
  readonly errorEstado = signal('');
  private readonly detalleDialog = viewChild.required<ElementRef<HTMLDialogElement>>('detalleDialog');
  private readonly estadoDialog = viewChild.required<ElementRef<HTMLDialogElement>>('estadoDialog');
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  readonly buscarUsuario = signal('');
  readonly buscarCabana = signal('');
  readonly usuariosFiltrados = computed(() => {
    const texto = this.buscarUsuario().trim().toLocaleLowerCase();
    return this.usuarios().filter(u => this.rolCuenta(u) === this.filtroRol()
      && ((u.nombreUsuario || u.nombre || '').toLocaleLowerCase().includes(texto)
      || u.email.toLocaleLowerCase().includes(texto)));
  });
  readonly cabanasFiltradas = computed(() => {
    const texto = this.buscarCabana().trim().toLocaleLowerCase();
    return this.cabanas().filter(c => c.nombre.toLocaleLowerCase().includes(texto));
  });

  crearUsuario(): void { this.nombreUsuarioOriginal.set(''); this.nombreUsuarioBloqueado.set(false); this.usuario = { ...this.nuevoUsuario(), rol: this.filtroRol() }; this.abrirEditor('usuario'); }
  crearCabana(): void { this.nueva(); this.abrirEditor('cabana'); }
  private abrirEditor(tipo: 'usuario' | 'cabana'): void {
    this.limpiar(); this.editor.set(tipo); this.cambios.detectChanges();
    if (!this.editorDialog().nativeElement.open) this.editorDialog().nativeElement.showModal();
  }
  cerrarEditor(): void {
    if (this.ocupado() || this.subiendoFotos()) return;
    this.editorDialog().nativeElement.close(); this.editor.set(null); this.limpiar();
  }
  readonly perfil = signal<any>(null);
  readonly cabanas = signal<any[]>([]);
  readonly usuarios = signal<Cuenta[]>([]);
  readonly seccionesCuentas: ReadonlyArray<{ rol: RolCuenta; titulo: string; singular: string }> = [
    { rol: 'CLIENTE', titulo: 'Clientes', singular: 'cliente' },
    { rol: 'TRABAJADOR', titulo: 'Trabajadores', singular: 'trabajador' },
    { rol: 'DUENO', titulo: 'Dueños', singular: 'dueño' },
    { rol: 'ADMIN', titulo: 'Administradores', singular: 'administrador' },
    { rol: 'RESTAURANTE', titulo: 'Personal de restaurante', singular: 'encargado de restaurante' },
  ];
  readonly mensaje = signal('');
  readonly error = signal('');
  readonly ocupado = signal(false);
  readonly bloqueos = signal<any[]>([]);
  tab = 'cabanas';
  cabana = this.nuevaCabana();
  imagenes = '';
  readonly subiendoFotos = signal(false);
  readonly guardandoFotoPerfil = signal(false);
  readonly vistaFotoPerfil = signal<string | null>(null);
  archivoFotoPerfil: File | null = null;
  usuario = this.nuevoUsuario();
  bloqueo = { idCabana: 0, desde: '', hasta: '', motivo: '' };
  passwordActual = '';
  passwordNueva = '';
  nuevaCabana(): any {
    return {
      idCabana: null,
      nombre: '',
      descripcion: '',
      aforoMaximo: 2,
      numHabitaciones: 1,
      numBanos: 1,
      tarifaNoche: 100,
      imagenPrincipal: '/images/cabana-placeholder.svg',
      duenoId: null,
    };
  }
  nuevoUsuario(): FormularioCuenta {
    return { id: null, nombreUsuario: '', email: '', password: '', rol: 'CLIENTE', persona: nuevaPersona() };
  }
  rol(): string {
    return this.perfil()?.rol === 'CLIENTE'
      ? 'CLIENTE'
      : (this.perfil()?.rol ?? '').replace('DUEÑO', 'DUENO');
  }
  es(...roles: string[]): boolean {
    return roles.includes(this.rol());
  }
  ngOnInit(): void {
    if (!this.auth.estaAutenticado()) {
      void this.router.navigate(['/login']);
      return;
    }
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      if (this.perfil()) this.seleccionarSeccion(params.get('seccion'));
    });
    this.http.get<any>(this.url + '/usuarios/me').subscribe({
      next: (p) => {
        this.perfil.set({ ...p, nombreUsuario: p.nombreUsuario || p.nombre });
        this.auth.actualizarPerfil(p);
        this.nombrePerfilOriginal.set(p.nombreUsuario || p.nombre);
        this.seleccionarSeccion(this.route.snapshot.queryParamMap.get('seccion'));
        this.cargar();
      },
      error: (e) => this.fallo(e),
    });
  }
  private seleccionarSeccion(seccion: string | null): void {
    const secciones = ['cabanas', 'cuentas', 'bloqueos', 'perfil'];
    this.tab = this.es('ADMIN') && secciones.includes(seccion ?? '') ? seccion! : this.es('ADMIN') ? 'cabanas' : 'perfil';
    this.limpiar();
    this.cambios.markForCheck();
  }
  cargar(): void {
    if (this.es('ADMIN'))
      this.http
        .get<any[]>(this.url + '/cabanas/gestion')
        .subscribe({ next: (x) => this.cabanas.set(x.map(c => ({ ...c,
          imagenPrincipal: imagenUrl(c.imagenPrincipal),
          imagenesSecundarias: (c.imagenesSecundarias ?? []).map(imagenUrl) }))), error: (e) => this.fallo(e) });
    if (this.es('ADMIN'))
      this.http
        .get<Cuenta[]>(this.url + '/usuarios')
        .subscribe({ next: (x) => this.usuarios.set(x), error: (e) => this.fallo(e) });
  }
  editarCabana(c: any): void {
    this.cabana = { ...c };
    this.imagenes = (c.imagenesSecundarias ?? []).join('\n');
    this.abrirEditor('cabana');
  }
  nueva(): void {
    this.cabana = this.nuevaCabana();
    this.imagenes = '';
    this.limpiar();
  }
  fotosAdicionales(): string[] {
    return this.imagenes.split('\n').map((x) => x.trim()).filter(Boolean);
  }

  quitarFoto(indice: number): void {
    this.pedirConfirmacion('Quitar foto de la galería', '¿Estás seguro de quitar esta foto? El cambio se aplicará al guardar la cabaña.', () => this.quitarFotoConfirmada(indice));
  }
  private quitarFotoConfirmada(indice: number): void {
    this.imagenes = this.fotosAdicionales().filter((_, i) => i !== indice).join('\n');
  }

  async seleccionarFotos(evento: Event, principal: boolean): Promise<void> {
    const input = evento.target as HTMLInputElement;
    const archivos = Array.from(input.files ?? []);
    input.value = '';
    if (!archivos.length || this.subiendoFotos()) return;
    if (!principal && this.fotosAdicionales().length + archivos.length > 8) {
      this.avisarError('Puedes agregar hasta 8 fotos adicionales.');
      return;
    }
    if (archivos.some((f) => !['image/jpeg', 'image/png'].includes(f.type)
        || f.size === 0 || f.size > 2 * 1024 * 1024)) {
      this.avisarError('Selecciona fotos JPG o PNG de hasta 2 MB cada una.');
      return;
    }
    this.pedirConfirmacion('Subir fotos de la cabaña', '¿Estás seguro de subir las fotos seleccionadas? Se guardarán en el servidor.', () => { void this.subirFotosConfirmadas(archivos, principal); });
  }
  private async subirFotosConfirmadas(archivos: File[], principal: boolean): Promise<void> {
    if (this.subiendoFotos() || this.ocupado()) return;
    this.limpiar();
    this.subiendoFotos.set(true);
    try {
      for (const archivo of archivos) {
        const datos = new FormData();
        datos.append('archivo', archivo);
        const respuesta = await firstValueFrom(this.http.post<{ url: string }>(
          this.url + '/cabanas/fotos', datos,
        ));
        const fotoUrl = new URL(respuesta.url, this.url).href;
        if (principal) this.cabana.imagenPrincipal = fotoUrl;
        else this.imagenes = [...this.fotosAdicionales(), fotoUrl].join('\n');
      }
    } catch (e) {
      this.fallo(e as HttpErrorResponse);
    } finally {
      this.subiendoFotos.set(false);
    }
  }
  guardarCabana(): void {
    if (this.ocupado() || this.subiendoFotos()) return;
    this.pedirConfirmacion(this.cabana.idCabana ? 'Guardar cambios de la cabaña' : 'Crear cabaña', '¿Estás seguro de guardar los datos de esta cabaña?', () => this.guardarCabanaConfirmada());
  }
  private guardarCabanaConfirmada(): void {
    if (this.ocupado() || this.subiendoFotos()) return;
    const id = this.cabana.idCabana;
    const body = {
      ...this.cabana,
      imagenesSecundarias: this.imagenes
        .split('\n')
        .map((x) => x.trim())
        .filter(Boolean),
    };
    this.ocupado.set(true);
    this.limpiar();
    const req = id
      ? this.http.put(this.url + '/cabanas/' + id, body)
      : this.http.post(this.url + '/cabanas', body);
    req.subscribe({
      next: () => {
        this.ocupado.set(false);
        this.cerrarEditor(); this.nueva();
        this.exito('Cabaña guardada.');
      },
      error: (e) => this.fallo(e),
    });
  }
  desactivarCabana(c: any): void {
    this.pedirConfirmacion('Desactivar cabaña', '¿Estás seguro de desactivar ' + c.nombre + '? Se conservará su historial.', () => this.desactivarCabanaConfirmada(c));
  }
  private desactivarCabanaConfirmada(c: any): void {
    this.ocupado.set(true); this.limpiar();
    this.http
      .delete(this.url + '/cabanas/' + c.idCabana)
      .subscribe({ next: () => this.exito('Cabaña desactivada.'), error: (e) => this.fallo(e) });
  }
  activar(c: any): void {
    this.pedirConfirmacion('Reactivar cabaña', '¿Estás seguro de reactivar ' + c.nombre + '?', () => this.activarConfirmada(c));
  }
  private activarConfirmada(c: any): void {
    this.ocupado.set(true); this.limpiar();
    this.http
      .put(this.url + '/cabanas/' + c.idCabana + '/activar', {})
      .subscribe({ next: () => this.exito('Cabaña activada.'), error: (e) => this.fallo(e) });
  }
  abrirCuentas(): void {
    this.tab = 'cuentas';
    this.usuario = this.nuevoUsuario();
    this.limpiar();
  }

  cuentasDe(rol: RolCuenta): Cuenta[] {
    return this.usuarios().filter((cuenta) => this.rolCuenta(cuenta) === rol);
  }

  rolCuenta(cuenta: Cuenta): string {
    switch (cuenta.rol.toUpperCase()) {
      case 'USUARIO': return 'CLIENTE';
      case 'DUEÑO':
      case 'PROPIETARIO': return 'DUENO';
      case 'EMPLEADO':
      case 'GUARDIAN': return 'TRABAJADOR';
      default: return cuenta.rol.toUpperCase();
    }
  }

  duenosActivos(): Cuenta[] {
    return this.cuentasDe('DUENO').filter((cuenta) => cuenta.activo);
  }

  nombreCuenta(id: number): string {
    return this.usuarios().find((cuenta) => cuenta.id === id)?.nombre ?? '#' + id;
  }

  editarUsuario(cuenta: Cuenta): void {
    this.nombreUsuarioOriginal.set(cuenta.nombreUsuario || cuenta.nombre);
    this.nombreUsuarioBloqueado.set(false);
    this.usuario = { ...cuenta, nombreUsuario: cuenta.nombreUsuario || cuenta.nombre,
      persona: cuenta.persona ? { ...cuenta.persona } : nuevaPersona(), rol: this.rolCuenta(cuenta), password: '' };
    this.abrirEditor('usuario');
  }

  guardarUsuario(): void {
    if (this.ocupado() || this.nombreUsuarioBloqueado()) return;
    const { id, nombreUsuario, email, rol, persona } = this.usuario;
    if (!nombreUsuario.trim() || (rol !== 'ADMIN' && Object.values(persona).some(v => !String(v).trim()))) {
      this.avisarError('Completa los campos obligatorios de la cuenta y de la persona.'); return;
    }
    const datos = { nombreUsuario: nombreUsuario.trim(), email: email.trim(), rol,
      ...(rol !== 'ADMIN' ? { persona } : {}) };
    if (id) {
      this.edicionPendiente.set({ id, datos: structuredClone(datos) });
      this.errorEdicion.set('');
      this.cambios.detectChanges();
      if (!this.edicionDialog().nativeElement.open) this.edicionDialog().nativeElement.showModal();
      return;
    }
    this.pedirConfirmacion('Crear usuario', '¿Estás seguro de crear esta cuenta?', () => this.guardarDatosUsuario(null, structuredClone(datos)));
  }
  cerrarConfirmacionEdicion(): void {
    if (this.ocupado()) return;
    this.edicionDialog().nativeElement.close();
    this.edicionPendiente.set(null);
    this.errorEdicion.set('');
  }
  confirmarEdicion(): void {
    const pendiente = this.edicionPendiente();
    if (!pendiente || this.ocupado()) return;
    this.guardarDatosUsuario(pendiente.id, pendiente.datos);
  }
  private guardarDatosUsuario(id: number | null, datos: { nombreUsuario: string; email: string; rol: string; persona?: Persona }): void {
    const req = id
      ? this.http.put(this.url + '/usuarios/' + id, datos)
      : this.http.post(this.url + '/usuarios', { ...datos, password: this.usuario.password });
    this.ocupado.set(true);
    this.limpiar();
    req.subscribe({
      next: () => {
        this.ocupado.set(false);
        this.cerrarConfirmacionEdicion();
        this.cerrarEditor();
        this.usuario = this.nuevoUsuario();
        this.exito('Cuenta guardada.');
      },
      error: (e: HttpErrorResponse) => {
        this.ocupado.set(false);
        const mensaje = e.error?.detail ?? e.error?.message ?? 'No se pudieron guardar los cambios. Inténtalo nuevamente.';
        if (id) { this.errorEdicion.set(mensaje); this.avisarError(mensaje); } else this.fallo(e);
        if (mensaje.toLocaleLowerCase().includes('nombre de usuario')) this.revisionNombreUsuario.update(n => n + 1);
      },
    });
  }

  verUsuario(cuenta: Cuenta): void {
    this.detalleUsuario.set({ ...cuenta, persona: cuenta.persona ? { ...cuenta.persona } : null });
    this.cambios.detectChanges();
    this.detalleDialog().nativeElement.showModal();
  }
  cerrarDetalle(): void {
    this.detalleDialog().nativeElement.close();
    this.detalleUsuario.set(null);
  }
  solicitarEstado(cuenta: Cuenta): void {
    if (this.ocupado() || cuenta.id === this.perfil()?.id || !this.es('ADMIN')) return;
    this.cuentaEstado.set({ ...cuenta });
    this.errorEstado.set('');
    this.cambios.detectChanges();
    this.estadoDialog().nativeElement.showModal();
  }
  cerrarEstado(): void {
    if (this.ocupado()) return;
    this.estadoDialog().nativeElement.close();
    this.cuentaEstado.set(null);
    this.errorEstado.set('');
  }
  confirmarEstado(): void {
    const cuenta = this.cuentaEstado();
    if (!cuenta || this.ocupado()) return;
    this.ocupado.set(true);
    this.errorEstado.set('');
    this.limpiar();
    this.http.put<Cuenta>(this.url + '/usuarios/' + cuenta.id + '/estado', { activo: !cuenta.activo }).subscribe({
      next: actualizada => {
        this.usuarios.update(cuentas => cuentas.map(u => u.id === cuenta.id ? actualizada : u));
        this.ocupado.set(false);
        this.cerrarEstado();
        this.avisarExito(actualizada.activo ? 'Cuenta reactivada.' : 'Cuenta desactivada.');
      },
      error: (e: HttpErrorResponse) => {
        this.ocupado.set(false);
        const mensaje = e.error?.detail ?? e.error?.message ?? 'No se pudo cambiar el estado. Inténtalo nuevamente.';
        this.errorEstado.set(mensaje); this.avisarError(mensaje);
      },
    });
  }
  consultarBloqueos(): void {
    if (!this.bloqueo.idCabana) return;
    this.http
      .get<any[]>(this.url + '/reservas/bloqueos', { params: { idCabana: this.bloqueo.idCabana } })
      .subscribe({ next: (x) => this.bloqueos.set(x), error: (e) => this.fallo(e) });
  }
  bloquear(): void {
    const b = this.bloqueo;
    if (!b.idCabana || !b.desde || !b.hasta || !b.motivo.trim()) { this.avisarError('Selecciona una cabaña y completa las fechas y el motivo.'); return; }
    if (b.hasta < b.desde) { this.avisarError('La fecha final no puede ser anterior a la fecha inicial.'); return; }
    const hoy = new Date().toLocaleDateString('sv-SE', { timeZone: 'America/Lima' });
    if (b.desde < hoy) { this.avisarError('No puedes bloquear fechas que ya pasaron. Selecciona hoy o una fecha posterior.'); return; }
    this.pedirConfirmacion('Bloquear fechas', '¿Estás seguro de bloquear esta cabaña del ' + b.desde + ' al ' + b.hasta + '?', () => this.bloquearConfirmado());
  }
  private bloquearConfirmado(): void {
    this.ocupado.set(true); this.limpiar();
    this.http.post(this.url + '/reservas/bloqueos', this.bloqueo).subscribe({
      next: () => {
        this.ocupado.set(false);
        this.avisarExito('Fechas bloqueadas.');
        this.consultarBloqueos();
      },
      error: (e) => this.fallo(e),
    });
  }
  desbloquear(b: any): void {
    this.pedirConfirmacion('Retirar bloqueo', '¿Estás seguro de retirar el bloqueo del ' + b.desde + ' al ' + b.hasta + '?', () => this.desbloquearConfirmado(b));
  }
  private desbloquearConfirmado(b: any): void {
    this.ocupado.set(true); this.limpiar();
    this.http
      .delete(this.url + '/reservas/bloqueos/' + b.id)
      .subscribe({ next: () => { this.ocupado.set(false); this.avisarExito('Bloqueo retirado.'); this.consultarBloqueos(); }, error: (e) => this.fallo(e) });
  }
  seleccionarFotoPerfil(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = '';
    if (!archivo || this.guardandoFotoPerfil()) return;
    if (!['image/jpeg', 'image/png'].includes(archivo.type)
        || archivo.size === 0 || archivo.size > 2 * 1024 * 1024) {
      this.avisarError('Selecciona una foto JPG o PNG de hasta 2 MB.');
      return;
    }
    this.cancelarFotoPerfil();
    this.limpiar();
    this.archivoFotoPerfil = archivo;
    const lector = new FileReader();
    lector.onload = () => {
      if (this.archivoFotoPerfil === archivo) this.vistaFotoPerfil.set(lector.result as string);
    };
    lector.readAsDataURL(archivo);
  }

  cancelarFotoPerfil(): void {
    this.vistaFotoPerfil.set(null);
    this.archivoFotoPerfil = null;
  }

  guardarFotoPerfil(): void {
    if (!this.archivoFotoPerfil || this.guardandoFotoPerfil()) return;
    this.pedirConfirmacion('Guardar foto de perfil', '¿Estás seguro de guardar esta foto de perfil?', () => this.guardarFotoPerfilConfirmada());
  }
  private guardarFotoPerfilConfirmada(): void {
    if (!this.archivoFotoPerfil || this.guardandoFotoPerfil()) return;
    const datos = new FormData();
    datos.append('file', this.archivoFotoPerfil);
    this.guardandoFotoPerfil.set(true);
    this.limpiar();
    this.http.post<Cuenta>(this.url + '/usuarios/' + this.perfil().id + '/foto', datos).subscribe({
      next: (perfil) => {
        this.perfil.set(perfil);
        this.auth.actualizarPerfil(perfil);
        this.cancelarFotoPerfil();
        this.guardandoFotoPerfil.set(false);
        this.avisarExito('Foto de perfil guardada.');
      },
      error: (e) => { this.guardandoFotoPerfil.set(false); this.fallo(e); },
    });
  }

  quitarFotoPerfil(): void {
    this.pedirConfirmacion('Quitar foto de perfil', '¿Estás seguro de quitar tu foto de perfil?', () => this.quitarFotoPerfilConfirmada());
  }
  private quitarFotoPerfilConfirmada(): void {
    if (this.guardandoFotoPerfil()) return;
    this.guardandoFotoPerfil.set(true);
    this.limpiar();
    this.http.delete(this.url + '/usuarios/me/foto').subscribe({
      next: () => {
        this.cancelarFotoPerfil();
        this.auth.cargarFotoPerfil();
        this.guardandoFotoPerfil.set(false);
        this.avisarExito('Foto de perfil eliminada.');
      },
      error: (e) => { this.guardandoFotoPerfil.set(false); this.fallo(e); },
    });
  }

  ngOnDestroy(): void { this.cancelarFotoPerfil(); }
  guardarPerfil(): void {
    if (this.nombrePerfilBloqueado() || this.ocupado()) return;
    this.pedirConfirmacion('Guardar perfil', '¿Estás seguro de guardar los cambios de tu perfil?', () => this.guardarPerfilConfirmado());
  }
  private guardarPerfilConfirmado(): void {
    if (this.nombrePerfilBloqueado()) return;
    this.ocupado.set(true); this.limpiar();
    const p = this.perfil();
    this.http.put(this.url + '/usuarios/' + p.id, { nombreUsuario: p.nombreUsuario, email: p.email, ...(p.persona ? { persona: p.persona } : {}) }).subscribe({
      next: (x: any) => {
        this.perfil.set(x);
        this.auth.actualizarPerfil(x);
        this.nombrePerfilOriginal.set(x.nombreUsuario || x.nombre);
        this.ocupado.set(false);
        this.avisarExito('Perfil guardado correctamente.');
      },
      error: (e) => this.fallo(e),
    });
  }
  cambiarPassword(): void {
    if (!this.passwordActual || this.passwordNueva.length < 8 || this.passwordNueva.length > 72) {
      this.avisarError('Ingresa tu contraseña actual y una nueva contraseña de entre 8 y 72 caracteres.'); return;
    }
    this.pedirConfirmacion('Cambiar contraseña', '¿Estás seguro de cambiar la contraseña de tu cuenta?', () => this.cambiarPasswordConfirmado());
  }
  private cambiarPasswordConfirmado(): void {
    this.ocupado.set(true); this.limpiar();
    this.http
      .put(this.url + '/usuarios/' + this.perfil().id + '/password', {
        password: this.passwordNueva,
        passwordActual: this.passwordActual,
      })
      .subscribe({
        next: () => {
          this.ocupado.set(false);
          this.passwordActual = '';
          this.passwordNueva = '';
          this.avisarExito('Contraseña actualizada.');
        },
        error: (e) => this.fallo(e),
      });
  }
  avisarExito(mensaje: string): void {
    this.error.set(''); this.mensaje.set(mensaje);
    this.notificaciones.mostrar('exito', mensaje);
  }
  avisarError(mensaje: string): void {
    this.mensaje.set(''); this.error.set(mensaje);
    this.notificaciones.mostrar('error', mensaje);
  }
  limpiar(): void {
    this.notificaciones.cerrar();
    this.mensaje.set('');
    this.error.set('');
  }
  exito(m: string): void {
    this.ocupado.set(false);
    this.avisarExito(m);
    this.cargar();
  }
  fallo(e: HttpErrorResponse): void {
    this.ocupado.set(false);
    this.avisarError(
      e.error?.detail ??
        e.error?.message ??
        (e.status === 403
          ? 'No tienes permiso para esta acción.'
          : e.status === 401
            ? 'Inicia sesión nuevamente.'
            : 'No se pudo completar la operación. Revisa los datos y los servicios.'),
    );
  }
}
