import { CommonModule } from '@angular/common';
import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth';
import { API_CONFIG } from '../../core/api.config';
type RolCuenta = 'CLIENTE' | 'TRABAJADOR' | 'DUENO' | 'ADMIN';

interface Cuenta {
  id: number;
  nombre: string;
  email: string;
  rol: string;
  activo: boolean;
  duenoId: number | null;
}

interface FormularioCuenta extends Omit<Cuenta, 'id' | 'activo'> {
  id: number | null;
  password: string;
}

@Component({
  selector: 'app-gestion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './gestion.component.html',
  styleUrl: './gestion.component.css',
})
export class GestionComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  readonly url = API_CONFIG.baseUrl;
  readonly perfil = signal<any>(null);
  readonly cabanas = signal<any[]>([]);
  readonly usuarios = signal<Cuenta[]>([]);
  readonly seccionesCuentas: ReadonlyArray<{ rol: RolCuenta; titulo: string; singular: string }> = [
    { rol: 'CLIENTE', titulo: 'Clientes', singular: 'cliente' },
    { rol: 'TRABAJADOR', titulo: 'Trabajadores', singular: 'trabajador' },
    { rol: 'DUENO', titulo: 'Dueños', singular: 'dueño' },
    { rol: 'ADMIN', titulo: 'Administradores', singular: 'administrador' },
  ];
  seccionCuenta = this.seccionesCuentas[0];
  asignacion: { cabanaId: number; ids: Set<number> } | null = null;
  readonly mensaje = signal('');
  readonly error = signal('');
  readonly ocupado = signal(false);
  readonly bloqueos = signal<any[]>([]);
  tab = 'cabanas';
  cabana = this.nuevaCabana();
  imagenes = '';
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
    return { id: null, nombre: '', email: '', password: '', rol: this.seccionCuenta.rol, duenoId: null };
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
    this.http.get<any>(this.url + '/usuarios/me').subscribe({
      next: (p) => {
        this.perfil.set(p);
        this.tab = this.es('ADMIN') ? 'cabanas' : 'perfil';
        this.cargar();
      },
      error: (e) => this.fallo(e),
    });
  }
  cargar(): void {
    if (this.es('ADMIN'))
      this.http
        .get<any[]>(this.url + '/cabanas/gestion')
        .subscribe({ next: (x) => this.cabanas.set(x), error: (e) => this.fallo(e) });
    if (this.es('ADMIN'))
      this.http
        .get<Cuenta[]>(this.url + '/usuarios')
        .subscribe({ next: (x) => this.usuarios.set(x), error: (e) => this.fallo(e) });
  }
  editarCabana(c: any): void {
    this.cabana = { ...c };
    this.imagenes = (c.imagenesSecundarias ?? []).join('\n');
    this.limpiar();
  }
  nueva(): void {
    this.cabana = this.nuevaCabana();
    this.imagenes = '';
    this.limpiar();
  }
  guardarCabana(): void {
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
        this.nueva();
        this.exito('Cabaña guardada.');
      },
      error: (e) => this.fallo(e),
    });
  }
  desactivarCabana(c: any): void {
    if (!confirm('¿Desactivar ' + c.nombre + '? Se conservará su historial.')) return;
    this.http
      .delete(this.url + '/cabanas/' + c.idCabana)
      .subscribe({ next: () => this.exito('Cabaña desactivada.'), error: (e) => this.fallo(e) });
  }
  activar(c: any): void {
    this.http
      .put(this.url + '/cabanas/' + c.idCabana + '/activar', {})
      .subscribe({ next: () => this.exito('Cabaña activada.'), error: (e) => this.fallo(e) });
  }
  abrirAsignacion(cabana: { idCabana: number; trabajadores?: number[] }): void {
    this.asignacion = { cabanaId: cabana.idCabana, ids: new Set(cabana.trabajadores ?? []) };
    this.limpiar();
  }

  cambiarAsignacion(id: number, seleccionado: boolean): void {
    if (!this.asignacion) return;
    if (seleccionado) this.asignacion.ids.add(id);
    else this.asignacion.ids.delete(id);
  }

  guardarAsignacion(): void {
    if (!this.asignacion || this.ocupado()) return;
    const { cabanaId, ids } = this.asignacion;
    this.ocupado.set(true);
    this.limpiar();
    this.http.put(this.url + '/cabanas/' + cabanaId + '/trabajadores', [...ids]).subscribe({
      next: () => {
        this.asignacion = null;
        this.exito('Trabajadores asignados.');
      },
      error: (e) => this.fallo(e),
    });
  }

  abrirCuentas(seccion: (typeof this.seccionesCuentas)[number]): void {
    this.tab = 'cuentas';
    this.seccionCuenta = seccion;
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

  trabajadoresDe(cabana: { duenoId: number | null; trabajadores?: number[] }): Cuenta[] {
    return this.cuentasDe('TRABAJADOR').filter((cuenta) =>
      (cuenta.activo && (cuenta.duenoId ?? null) === (cabana.duenoId ?? null))
      || cabana.trabajadores?.includes(cuenta.id),
    );
  }

  nombreCuenta(id: number): string {
    return this.usuarios().find((cuenta) => cuenta.id === id)?.nombre ?? '#' + id;
  }

  nombresTrabajadores(ids: number[]): string {
    return ids.map((id) => this.nombreCuenta(id)).join(', ');
  }

  cabanasDe(cuenta: Cuenta): string {
    return this.cabanas()
      .filter((cabana) => cabana.duenoId === cuenta.id || cabana.trabajadores?.includes(cuenta.id))
      .map((cabana) => cabana.nombre)
      .join(', ');
  }

  editarUsuario(cuenta: Cuenta): void {
    this.usuario = { ...cuenta, rol: this.rolCuenta(cuenta), password: '' };
    this.limpiar();
  }

  guardarUsuario(): void {
    if (this.ocupado()) return;
    const { id, nombre, email, password, rol, duenoId } = this.usuario;
    const datos = { nombre: nombre.trim(), email: email.trim(), rol, duenoId };
    const req = id
      ? this.http.put(this.url + '/usuarios/' + id, datos)
      : this.http.post(this.url + '/usuarios', { ...datos, password });
    this.ocupado.set(true);
    this.limpiar();
    req.subscribe({
      next: () => {
        this.usuario = this.nuevoUsuario();
        this.exito('Cuenta guardada.');
      },
      error: (e) => this.fallo(e),
    });
  }

  eliminarUsuario(cuenta: Cuenta): void {
    if (this.ocupado()) return;
    const desactivar = this.rolCuenta(cuenta) === 'ADMIN';
    const pregunta = desactivar
      ? '¿Desactivar la cuenta de ' + cuenta.email + '?'
      : '¿Eliminar definitivamente la cuenta de ' + cuenta.email
        + '? No podrás recuperarla. Se conservará el historial de reservas.'
        + (this.rolCuenta(cuenta) === 'DUENO'
          ? ' Sus trabajadores quedarán sin dueño y sus cabañas pasarán a administración.'
          : this.rolCuenta(cuenta) === 'TRABAJADOR'
            ? ' Se retirarán sus asignaciones a cabañas.'
            : '');
    if (!confirm(pregunta)) return;
    this.ocupado.set(true);
    this.limpiar();
    this.http.delete(this.url + '/usuarios/' + cuenta.id).subscribe({
      next: () => {
        if (this.usuario.id === cuenta.id) this.usuario = this.nuevoUsuario();
        this.exito(desactivar ? 'Cuenta desactivada.' : 'Cuenta eliminada definitivamente.');
      },
      error: (e) => this.fallo(e),
    });
  }
  consultarBloqueos(): void {
    if (!this.bloqueo.idCabana) return;
    this.http
      .get<any[]>(this.url + '/reservas/bloqueos', { params: { idCabana: this.bloqueo.idCabana } })
      .subscribe({ next: (x) => this.bloqueos.set(x), error: (e) => this.fallo(e) });
  }
  bloquear(): void {
    this.http.post(this.url + '/reservas/bloqueos', this.bloqueo).subscribe({
      next: () => {
        this.mensaje.set('Fechas bloqueadas.');
        this.consultarBloqueos();
      },
      error: (e) => this.fallo(e),
    });
  }
  desbloquear(b: any): void {
    this.http
      .delete(this.url + '/reservas/bloqueos/' + b.id)
      .subscribe({ next: () => this.consultarBloqueos(), error: (e) => this.fallo(e) });
  }
  guardarPerfil(): void {
    const p = this.perfil();
    this.http.put(this.url + '/usuarios/' + p.id, { nombre: p.nombre, email: p.email }).subscribe({
      next: (x) => {
        this.perfil.set(x);
        this.mensaje.set('Perfil guardado. Inicia sesión nuevamente si cambiaste el correo.');
      },
      error: (e) => this.fallo(e),
    });
  }
  cambiarPassword(): void {
    this.http
      .put(this.url + '/usuarios/' + this.perfil().id + '/password', {
        password: this.passwordNueva,
        passwordActual: this.passwordActual,
      })
      .subscribe({
        next: () => {
          this.passwordActual = '';
          this.passwordNueva = '';
          this.mensaje.set('Contraseña actualizada.');
        },
        error: (e) => this.fallo(e),
      });
  }
  limpiar(): void {
    this.mensaje.set('');
    this.error.set('');
  }
  exito(m: string): void {
    this.ocupado.set(false);
    this.mensaje.set(m);
    this.cargar();
  }
  fallo(e: HttpErrorResponse): void {
    this.ocupado.set(false);
    this.error.set(
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
