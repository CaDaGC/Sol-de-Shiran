import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_CONFIG } from '../core/api.config';
import { Cuenta } from '../models/usuario.model';

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private readonly http = inject(HttpClient);
  listar() { return this.http.get<Cuenta[]>(API_CONFIG.baseUrl + '/usuarios'); }
  perfil() { return this.http.get<Cuenta>(API_CONFIG.baseUrl + '/usuarios/me'); }
}
