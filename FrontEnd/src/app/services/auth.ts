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

  readonly usuarioActual = signal<UsuarioAutenticado | null>(
    this.leerUsuarioGuardado()
  );

  constructor(private readonly http: HttpClient) {}

  login(credentials: { email: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.url, credentials);
  }

  guardarSesion(response: AuthResponse): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const usuario: UsuarioAutenticado = {
      nombre: response.nombre || 'Usuario',
      email: response.email || '',
      rol: response.rol || 'Usuario'
    };

    localStorage.setItem('token', response.token);
    localStorage.setItem('usuario', JSON.stringify(usuario));
    this.usuarioActual.set(usuario);
  }

  cerrarSesion(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem('token');
      localStorage.removeItem('usuario');
    }

    this.usuarioActual.set(null);
  }

  estaAutenticado(): boolean {
    return isPlatformBrowser(this.platformId) &&
      Boolean(localStorage.getItem('token'));
  }

  private leerUsuarioGuardado(): UsuarioAutenticado | null {
    if (!isPlatformBrowser(this.platformId)) return null;

    const guardado = localStorage.getItem('usuario');
    const token = localStorage.getItem('token');

    if (guardado) {
      try {
        const dato = JSON.parse(guardado) as Record<string, unknown>;
        const normalizado = this.normalizarUsuario(dato);

        if (normalizado.email || normalizado.nombre !== 'Usuario') {
          return normalizado;
        }
      } catch {
        // Si el dato guardado no es JSON válido, se intenta recuperar del token.
      }
    }

    if (token) {
      const claims = this.leerClaims(token);
      if (claims) return this.normalizarUsuario(claims);
    }

    return null;
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

  private leerClaims(token: string): Record<string, unknown> | null {
    try {
      const partePayload = token.split('.')[1];
      if (!partePayload) return null;

      const base64 = partePayload
        .replace(/-/g, '+')
        .replace(/_/g, '/')
        .padEnd(Math.ceil(partePayload.length / 4) * 4, '=');

      const binario = atob(base64);
      const bytes = Uint8Array.from(binario, (caracter) =>
        caracter.charCodeAt(0)
      );

      return JSON.parse(
        new TextDecoder().decode(bytes)
      ) as Record<string, unknown>;
    } catch {
      return null;
    }
  }

  private aTexto(valor: unknown): string {
    return typeof valor === 'string' ? valor.trim() : '';
  }
}
