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
  private readonly userNameKey = 'anexo24_user_name';
  private readonly http = inject(HttpClient);
  readonly userName = signal<string | null>(localStorage.getItem(this.userNameKey));

  login(credentials: LoginCredentials): Observable<LoginResponseDto> {
    return this.http.post<LoginResponseDto>('/api/v1/auth/login', { clave: credentials.username, password: credentials.password }).pipe(
      tap((resp) => {
        localStorage.setItem(this.tokenKey, resp.token);
        const userName = resp.usuario?.trim() || credentials.username;
        localStorage.setItem(this.userNameKey, userName);
        this.userName.set(userName);
      }),
    );
  }

  token(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  /** Devuelve hasta dos iniciales para identificar la sesión sin cargar imágenes externas. */
  initials(): string {
    const name = (this.userName() || 'Usuario').trim();
    const words = name.split(/\s+/).filter(Boolean);

    if (words.length > 1) {
      return `${words[0][0]}${words[words.length - 1][0]}`.toUpperCase();
    }

    return name.slice(0, 2).toUpperCase();
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userNameKey);
    this.userName.set(null);
  }

  tieneSesion(): boolean {
    return this.token() !== null;
  }
}
