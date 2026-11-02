import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { Observable } from 'rxjs';
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

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly url = `${API_CONFIG.baseUrl}/auth/login`;
  private readonly platformId = inject(PLATFORM_ID);
  private temporizadorExpiracion?: ReturnType<typeof setTimeout>;

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
      rol: response.rol?.trim() || 'Usuario'
    };

    localStorage.setItem('token', response.token);
    localStorage.setItem('usuario', JSON.stringify(usuario));
    this.usuarioActual.set(usuario);
    this.programarExpiracion(response.token);
  }

  estaAutenticado(): boolean {
    if (!isPlatformBrowser(this.platformId)) return false;

    const token = localStorage.getItem('token');

    if (!token || !this.tokenVigente(token)) {
      this.cerrarSesion();
      return false;
    }

    if (!this.usuarioActual()) {
      this.restaurarUsuario(token);
    }

    return true;
  }

  cerrarSesion(): void {
    this.cancelarTemporizador();

    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem('token');
      localStorage.removeItem('usuario');
    }

    this.usuarioActual.set(null);
  }

  private restaurarSesion(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const token = localStorage.getItem('token');

    if (!token || !this.tokenVigente(token)) {
      this.cerrarSesion();
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
        // Si el valor local no es JSON válido, recupera lo disponible del JWT.
      }
    }

    const claims = this.leerClaims(token);

    if (claims) {
      this.usuarioActual.set(this.normalizarUsuario(claims));
    }
  }

  private tokenVigente(token: string): boolean {
    const claims = this.leerClaims(token);
    const expiracion = claims?.['exp'];

    // El backend emite tokens JWT con el claim exp.
    if (typeof expiracion !== 'number') return false;

    return expiracion * 1000 > Date.now();
  }

  private programarExpiracion(token: string): void {
    this.cancelarTemporizador();

    const claims = this.leerClaims(token);
    const expiracion = claims?.['exp'];

    if (typeof expiracion !== 'number') {
      this.cerrarSesion();
      return;
    }

    const milisegundosRestantes = expiracion * 1000 - Date.now();

    if (milisegundosRestantes <= 0) {
      this.cerrarSesion();
      return;
    }

    // El límite permite volver a comprobar tokens con duraciones largas.
    const maximoTemporizador = 2_000_000_000;

    this.temporizadorExpiracion = setTimeout(() => {
      if (!this.tokenVigente(token)) {
        this.cerrarSesion();
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

    return { nombre, email, rol };
  }

  private aTexto(valor: unknown): string {
    return typeof valor === 'string' ? valor.trim() : '';
  }
}
