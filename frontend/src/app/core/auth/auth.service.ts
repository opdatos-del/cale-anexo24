import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface LoginCredentials {
  username: string;
  password: string;
}

interface LoginResponseDto {
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
  readonly userName = signal<string | null>(null);

  login(credentials: LoginCredentials): Observable<LoginResponseDto> {
    return this.http.post<LoginResponseDto>('/api/v1/auth/login', { clave: credentials.username, password: credentials.password }).pipe(
      tap((resp) => {
        localStorage.setItem(this.tokenKey, resp.token);
        this.userName.set(resp.usuario);
      }),
    );
  }

  token(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    this.userName.set(null);
  }

  tieneSesion(): boolean {
    return this.token() !== null;
  }
}
