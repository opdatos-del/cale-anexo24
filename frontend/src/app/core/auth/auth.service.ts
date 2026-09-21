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
  private readonly permissionsKey = 'anexo24_permissions';
  private readonly http = inject(HttpClient);
  readonly userName = signal<string | null>(localStorage.getItem(this.userNameKey));
  /** Permisos conservados sólo para UX; backend sigue siendo la frontera de seguridad. */
  readonly permissions = signal<string[]>(this.readPermissions());

  login(credentials: LoginCredentials): Observable<LoginResponseDto> {
    return this.http.post<LoginResponseDto>('/api/v1/auth/login', { clave: credentials.username, password: credentials.password }).pipe(
      tap((resp) => {
        localStorage.setItem(this.tokenKey, resp.token);
        const userName = resp.usuario?.trim() || credentials.username;
        localStorage.setItem(this.userNameKey, userName);
        localStorage.setItem(this.permissionsKey, JSON.stringify(resp.permisos ?? []));
        this.userName.set(userName);
        this.permissions.set(resp.permisos ?? []);
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

  hasPermission(permission: string): boolean {
    return this.permissions().includes(permission);
  }

  hasAnyPermission(...permissions: string[]): boolean {
    return permissions.some((permission) => this.hasPermission(permission));
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userNameKey);
    localStorage.removeItem(this.permissionsKey);
    this.userName.set(null);
    this.permissions.set([]);
  }

  tieneSesion(): boolean {
    return this.token() !== null;
  }

  private readPermissions(): string[] {
    try {
      const stored = localStorage.getItem(this.permissionsKey);
      const parsed: unknown = stored ? JSON.parse(stored) : [];
      return Array.isArray(parsed) && parsed.every((permission) => typeof permission === 'string')
        ? parsed
        : [];
    } catch {
      return [];
    }
  }
}
