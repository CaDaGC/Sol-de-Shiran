import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_CONFIG } from '../core/api.config';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private url = `${API_CONFIG.baseUrl}/auth/login`;

  constructor(private http: HttpClient) {}

  login(credentials: { email: string; password: string }): Observable<any> {
    return this.http.post<any>(this.url, credentials);
  }
}
