import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_CONFIG } from '../core/api.config';

export interface DisponibilidadNombre {
  nombre: string;
  disponible: boolean;
  sugerencias: string[];
}

@Injectable({ providedIn: 'root' })
export class NombreUsuarioService {
  private readonly http = inject(HttpClient);

  consultar(nombre: string) {
    return this.http.get<DisponibilidadNombre>(API_CONFIG.baseUrl + '/usuarios/nombre-disponible', {
      params: { nombre },
    });
  }
}
