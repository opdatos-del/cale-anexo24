import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '@core/auth/auth.service';
import { LoginPage } from './login.page';

describe('LoginPage', () => {
  const auth = { login: vi.fn() };
  const router = { navigate: vi.fn() };

  beforeEach(() => {
    auth.login.mockReset();
    router.navigate.mockReset();
    TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    });
  });

  it('navega al dashboard después de un login exitoso', () => {
    auth.login.mockReturnValue(of({ token: 'token-test', expiraEn: 60, usuario: 'E2E_TEST', permisos: [] }));
    const fixture = TestBed.createComponent(LoginPage);
    const page = fixture.componentInstance as unknown as {
      form: { setValue(value: { username: string; password: string }): void };
      enviar(): void;
    };
    page.form.setValue({ username: 'E2E_TEST', password: 'secret' });

    page.enviar();

    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(router.navigate).not.toHaveBeenCalledWith(['/materiales']);
  });

  it('no navega cuando el login falla', () => {
    auth.login.mockReturnValue(throwError(() => new Error('credenciales inválidas')));
    const fixture = TestBed.createComponent(LoginPage);
    const page = fixture.componentInstance as unknown as {
      form: { setValue(value: { username: string; password: string }): void };
      enviar(): void;
    };
    page.form.setValue({ username: 'E2E_TEST', password: 'incorrecta' });

    page.enviar();

    expect(router.navigate).not.toHaveBeenCalled();
  });
});
