import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from './auth.service';

/**
 * Pantalla de inicio de sesión (CU-001).
 *
 * Responsabilidad: capturar credenciales y delegar la autenticación a
 * {@link AuthService}. El componente no ejecuta llamadas HTTP.
 */
@Component({
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
  selector: 'app-login',
  template: `
    <div class="relative flex min-h-screen w-full items-center justify-center overflow-hidden">
      <!-- Imagen de fondo: public/assets/login-bg.svg (sustituir por fotografía corporativa) -->
      <img
        src="/assets/login-bg.svg"
        alt=""
        class="absolute inset-0 h-full w-full object-cover"
        aria-hidden="true"
      />
      <!-- Overlay oscuro para contraste -->
      <div class="absolute inset-0 bg-slate-950/45" aria-hidden="true"></div>

      <div
        class="relative z-10 w-[calc(100%-2rem)] max-w-[400px] animate-card-in rounded-2xl border border-white/15 bg-white/92 p-8 shadow-[0_20px_60px_-15px_rgba(0,0,0,0.5)] backdrop-blur-md"
      >
        <!-- Encabezado -->
        <header class="mb-8 text-center">
          <div
            class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-primary text-on-primary shadow-md"
          >
            <mat-icon>inventory_2</mat-icon>
          </div>
          <h1 class="m-0 text-2xl font-semibold tracking-tight text-on-surface">Anexo 24</h1>
          <p class="m-0 mt-1 text-sm text-on-surface-variant">Sistema de control aduanero</p>
          <p class="m-0 mt-4 text-sm text-on-surface-variant">
            Ingresa tus credenciales para continuar
          </p>
        </header>

        <!-- Formulario -->
        <form [formGroup]="form" (ngSubmit)="enviar()" class="flex flex-col gap-4" novalidate>
          <mat-form-field appearance="outline" class="w-full">
            <mat-label for="username">Usuario</mat-label>
            <input
              id="username"
              matInput
              formControlName="username"
              placeholder="usuario"
              autocomplete="username"
              (input)="limpiarError()"
            />
            @if (mostrarError(username)) {
              <mat-error>{{
                username.hasError('required') ? 'El usuario es obligatorio.' : ''
              }}</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline" class="w-full">
            <mat-label for="password">Contraseña</mat-label>
            <input
              id="password"
              matInput
              formControlName="password"
              placeholder="••••••••"
              [type]="ocultarPassword() ? 'password' : 'text'"
              autocomplete="current-password"
              (input)="limpiarError()"
            />
            <button
              mat-icon-button
              type="button"
              class="mr-1"
              [attr.aria-label]="ocultarPassword() ? 'Mostrar contraseña' : 'Ocultar contraseña'"
              [attr.aria-pressed]="!ocultarPassword()"
              (click)="alternarPassword()"
            >
              <mat-icon>{{ ocultarPassword() ? 'visibility' : 'visibility_off' }}</mat-icon>
            </button>
            @if (mostrarError(password)) {
              <mat-error>{{
                password.hasError('required') ? 'La contraseña es obligatoria.' : ''
              }}</mat-error>
            }
          </mat-form-field>

          @if (error()) {
            <p
              role="alert"
              class="m-0 flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
            >
              <mat-icon class="text-[18px]">error_outline</mat-icon>
              <span>{{ error() }}</span>
            </p>
          }

          <button
            mat-flat-button
            color="primary"
            type="submit"
            [disabled]="cargando()"
            class="!h-12 w-full !text-base"
          >
            @if (cargando()) {
              <span class="flex items-center justify-center gap-2">
                <span
                  class="inline-block h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent"
                  aria-hidden="true"
                ></span>
                <span>Iniciando sesión...</span>
              </span>
            } @else {
              <span>Iniciar sesión</span>
            }
          </button>
        </form>
      </div>
    </div>
  `,
})
export class LoginComponent {
  protected readonly username = new FormControl('', {
    validators: [Validators.required],
    nonNullable: true,
  });
  protected readonly password = new FormControl('', {
    validators: [Validators.required],
    nonNullable: true,
  });
  protected readonly form = new FormGroup({
    username: this.username,
    password: this.password,
  });
  protected readonly ocultarPassword = signal(true);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  /** true tras el primer intento de envío: activa la validación visible. */
  protected readonly enviado = signal(false);

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** El error solo se muestra tras tocar el campo o intentar enviar. */
  protected mostrarError(campo: FormControl<string>): boolean {
    return campo.invalid && (campo.touched || this.enviado());
  }

  protected alternarPassword(): void {
    this.ocultarPassword.update((v) => !v);
  }

  protected limpiarError(): void {
    this.error.set(null);
  }

  protected enviar(): void {
    this.error.set(null);
    this.enviado.set(true);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    this.auth
      .login({ clave: this.username.value, password: this.password.value })
      .subscribe({
        next: () => this.router.navigate(['/materiales']),
        error: (err) => {
          this.cargando.set(false);
          this.error.set(this.mensajeError(err));
        },
      });
  }

  private mensajeError(err: unknown): string {
    const code = (err as { error?: { code?: string } }).error?.code;
    return code === 'CREDENCIALES_INVALIDAS'
      ? 'Usuario o contraseña incorrectos.'
      : 'No fue posible iniciar sesión. Intenta nuevamente.';
  }
}