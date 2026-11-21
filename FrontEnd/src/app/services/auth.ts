import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { inject, Injectable, Injector, PLATFORM_ID, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { Cuenta } from '../models/usuario.model';
import { imagenUrl } from '../core/imagen-url';
import { API_CONFIG } from '../core/api.config';

export interface UsuarioAutenticado {
  nombre: string;
  email: string;
  rol: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  nombre: string;
  email: string;
  rol: string;
}

export function normalizarRol(rol: string): string {
  switch (rol.trim().toUpperCase()) {
    case 'USUARIO': return 'CLIENTE';
    case 'GUARDIAN':
    case 'EMPLEADO': return 'TRABAJADOR';
    case 'DUEÑO':
    case 'PROPIETARIO': return 'DUENO';
    default: return rol.trim().toUpperCase();
  }
}

export function inicioPorRol(rol?: string): string {
  switch (normalizarRol(rol ?? '')) {
    case 'TRABAJADOR': return '/guardian/reservas';
    case 'CLIENTE': return '/mis-reservas';
    case 'DUENO': return '/inicio-dueno';
    case 'ADMIN':
    case '': return '/catalogo';
    default: return '/inicio-dueno';
  }
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly url = `${API_CONFIG.baseUrl}/auth/login`;
  private readonly platformId = inject(PLATFORM_ID);
  private readonly injector = inject(Injector);
  private temporizadorExpiracion?: ReturnType<typeof setTimeout>;

  readonly fotoPerfil = signal<string | null>(null);
  private solicitudFoto = 0;

  readonly usuarioActual = signal<UsuarioAutenticado | null>(null);

  constructor(private readonly http: HttpClient) {
    this.restaurarSesion();
  }

  login(
    credentials: { email: string; password: string }
  ): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.url, credentials);
  }

  guardarSesion(response: AuthResponse): void {
    if (!isPlatformBrowser(this.platformId)) return;

    if (!response?.token || !this.tokenVigente(response.token)) {
      this.cerrarSesion();
      return;
    }

    const usuario: UsuarioAutenticado = {
      nombre: response.nombre?.trim() || 'Usuario',
      email: response.email?.trim() || '',
      rol: normalizarRol(response.rol ?? '')
    };

    sessionStorage.removeItem('sesionExpirada');
    localStorage.setItem('token', response.token);
    localStorage.setItem('usuario', JSON.stringify(usuario));
    this.usuarioActual.set(usuario);
    this.programarExpiracion(response.token);
    this.cargarFotoPerfil();
  }

  estaAutenticado(): boolean {
    if (!isPlatformBrowser(this.platformId)) return false;

    const token = localStorage.getItem('token');

    if (!token || !this.tokenVigente(token)) {
      if (token) this.expirarSesion(); else this.cerrarSesion();
      return false;
    }

    if (!this.usuarioActual()) {
      this.restaurarUsuario(token);
    }

    return true;
  }

  cerrarSesion(): void {
    this.cancelarTemporizador();
    this.solicitudFoto++;
    this.establecerFoto(null);

    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem('token');
      localStorage.removeItem('usuario');
      sessionStorage.removeItem('sesionExpirada');
    }

    this.usuarioActual.set(null);
  }

  expirarSesion(): void {
    this.cerrarSesion();
    if (!isPlatformBrowser(this.platformId)) return;
    sessionStorage.setItem('sesionExpirada', 'true');

    queueMicrotask(() => {
      if (!this.usuarioActual()) void this.injector.get(Router).navigate(['/login'], {
        queryParams: { sesion: 'expirada' }, replaceUrl: true
      });
    });
  }

  cargarFotoPerfil(): void {
    if (!this.estaAutenticado()) return;
    const solicitud = ++this.solicitudFoto;
    this.http.get<Cuenta>(`${API_CONFIG.baseUrl}/usuarios/me`).subscribe({
      next: (perfil) => {
        if (solicitud === this.solicitudFoto) this.actualizarPerfil(perfil);
      },
      error: () => {
        if (solicitud === this.solicitudFoto) this.establecerFoto(null);
      },
    });
  }

  actualizarPerfil(perfil: Cuenta): void {
    this.solicitudFoto++;
    const usuario = { nombre: perfil.nombreUsuario || perfil.nombre, email: perfil.email,
      rol: normalizarRol(perfil.rol) };
    this.usuarioActual.set(usuario);
    this.establecerFoto(perfil.fotoPerfil ? imagenUrl(perfil.fotoPerfil) : null);
    if (isPlatformBrowser(this.platformId)) localStorage.setItem('usuario', JSON.stringify(usuario));
  }

  private establecerFoto(url: string | null): void { this.fotoPerfil.set(url); }
  private restaurarSesion(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const token = localStorage.getItem('token');

    if (!token || !this.tokenVigente(token)) {
      if (token) this.expirarSesion(); else this.cerrarSesion();
      return;
    }

    this.restaurarUsuario(token);
    this.programarExpiracion(token);
  }

  private restaurarUsuario(token: string): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const usuarioGuardado = localStorage.getItem('usuario');

    if (usuarioGuardado) {
      try {
        const guardado = JSON.parse(usuarioGuardado) as Record<string, unknown>;
        const normalizado = this.normalizarUsuario(guardado);

        if (normalizado.email || normalizado.nombre !== 'Usuario') {
          this.usuarioActual.set(normalizado);
          return;
        }
      } catch {

      }
    }

    const claims = this.leerClaims(token);

    if (claims) {
      this.usuarioActual.set(this.normalizarUsuario(claims));
    }
  }

  private expiracionToken(token: string): number {
    const claims = this.leerClaims(token);
    const exp = claims?.['exp'], iat = claims?.['iat'];
    if (typeof exp !== 'number') return 0;

    return typeof iat === 'number' ? Math.min(exp, iat + 3600) : exp;
  }

  private tokenVigente(token: string): boolean {
    return this.expiracionToken(token) * 1000 > Date.now();
  }

  private programarExpiracion(token: string): void {
    this.cancelarTemporizador();

    const expiracion = this.expiracionToken(token);

    if (!expiracion) {
      this.cerrarSesion();
      return;
    }

    const milisegundosRestantes = expiracion * 1000 - Date.now();

    if (milisegundosRestantes <= 0) {
      this.expirarSesion();
      return;
    }


    const maximoTemporizador = 2_000_000_000;

    this.temporizadorExpiracion = setTimeout(() => {
      if (!this.tokenVigente(token)) {
        this.expirarSesion();
        return;
      }

      this.programarExpiracion(token);
    }, Math.min(milisegundosRestantes, maximoTemporizador));
  }

  private cancelarTemporizador(): void {
    if (this.temporizadorExpiracion) {
      clearTimeout(this.temporizadorExpiracion);
      this.temporizadorExpiracion = undefined;
    }
  }

  private leerClaims(token: string): Record<string, unknown> | null {
    try {
      const payload = token.split('.')[1];
      if (!payload) return null;

      const base64 = payload
        .replace(/-/g, '+')
        .replace(/_/g, '/')
        .padEnd(Math.ceil(payload.length / 4) * 4, '=');

      const binario = atob(base64);
      const bytes = Uint8Array.from(binario, caracter =>
        caracter.charCodeAt(0)
      );

      return JSON.parse(
        new TextDecoder().decode(bytes)
      ) as Record<string, unknown>;
    } catch {
      return null;
    }
  }

  private normalizarUsuario(
    dato: Record<string, unknown>
  ): UsuarioAutenticado {
    const anidado =
      (dato['usuario'] as Record<string, unknown> | undefined) ??
      (dato['user'] as Record<string, unknown> | undefined) ??
      dato;

    const email =
      this.aTexto(anidado['email']) ||
      this.aTexto(anidado['sub']);

    const nombre =
      this.aTexto(anidado['nombre']) ||
      this.aTexto(anidado['name']) ||
      (email ? email.split('@')[0] : 'Usuario');

    const rol =
      this.aTexto(anidado['rol']) ||
      this.aTexto(anidado['role']) ||
      'Usuario';

    return { nombre, email, rol: normalizarRol(rol) };
  }

  private aTexto(valor: unknown): string {
    return typeof valor === 'string' ? valor.trim() : '';
  }
}
