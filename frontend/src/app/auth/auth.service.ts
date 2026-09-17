import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface LoginRequest {
  clave: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  expiraEn: number;
  usuario: string;
  permisos: string[];
}

/**
 * Gestión de autenticación: login, token y sesión en memoria.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'anexo24_token';
  private readonly http = inject(HttpClient);
  readonly usuario = signal<string | null>(null);

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/v1/auth/login', request).pipe(
      tap((resp) => {
        localStorage.setItem(this.tokenKey, resp.token);
        this.usuario.set(resp.usuario);
      }),
    );
  }

  token(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    this.usuario.set(null);
  }

  tieneSesion(): boolean {
    return this.token() !== null;
  }
}