import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';
import { API_CONFIG } from '../core/api.config';
import { Cabana, CabanaDetalle } from '../models/cabana.model';

@Injectable({
  providedIn: 'root'
})
export class CabanaService {
  private readonly url = `${API_CONFIG.baseUrl}/cabanas`;

  constructor(private readonly http: HttpClient) {}

  listarCabanas(): Observable<Cabana[]> {
    return this.http.get<Cabana[]>(this.url).pipe(
      timeout({ first: 15000 })
    );
  }

  obtenerDetalle(idCabana: number): Observable<CabanaDetalle> {
    return this.http.get<CabanaDetalle>(`${this.url}/${idCabana}`).pipe(
      timeout({ first: 15000 })
    );
  }
}
