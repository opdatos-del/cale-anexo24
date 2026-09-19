import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../../../core/auth/auth.service';
import { AppAlertComponent } from '../../../../../core/ui/app-alert/app-alert.component';
import { AppBrandLoaderComponent } from '../../../../../core/ui/app-brand-loader/app-brand-loader.component';

/**
 * Pantalla de inicio de sesión (CU-001).
 *
 * Responsabilidad: capturar credenciales y delegar la autenticación a
 * {@link AuthService}. El componente no ejecuta llamadas HTTP.
 */
@Component({
  imports: [AppAlertComponent, AppBrandLoaderComponent, ReactiveFormsModule],
  selector: 'app-login',
  template: `
    <div class="relative min-h-screen w-full overflow-hidden">
      <app-brand-loader [visible]="isLoading()" label="Iniciando sesión..." />

      <!-- FONDO: cubre todo el viewport -->
      <div class="absolute inset-0">
        <!-- Imagen: public/assets/login-bg.jpg (sustituir por fotografía corporativa propia).
             Actual: "Warehouse interior showcasing organized shelving and packages" de
             Shixart1985, Wikimedia Commons, CC BY 2.0 (https://creativecommons.org/licenses/by/2.0) -->
        <img src="/assets/login-bg.jpg" alt="" class="h-full w-full object-cover" />
      </div>

      <!-- OVERLAY para contraste -->
      <div class="absolute inset-0 bg-slate-950/45"></div>

      <!-- CONTENEDOR CENTRADO: card flotante encima de fondo + overlay -->
      <div class="relative z-10 flex min-h-screen w-full items-center justify-center p-4">
        <div
          class="animate-card-in w-full max-w-105 rounded-3xl border border-white/20 bg-white/90 p-8 shadow-2xl backdrop-blur-xl"
        >
          <!-- HEADER -->
          <header class="text-center">
            <img class="mx-auto h-24 w-64 object-contain" src="/logo_sin_fondo.png" alt="Anexo 24" />
            <p class="mt-1 text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Control de inventarios</p>
            <p class="mt-4 text-sm text-slate-500">Ingresa tus credenciales para continuar</p>
          </header>

          <!-- FORMULARIO: estrictamente vertical -->
          <form [formGroup]="form" (ngSubmit)="enviar()" class="mt-8 space-y-5" novalidate>
            <div>
              <label for="username" class="mb-1.5 block text-sm font-medium text-slate-700">
                Usuario
              </label>
              <input
                id="username"
                type="text"
                formControlName="username"
                placeholder="usuario"
                autocomplete="username"
                class="w-full rounded-xl border border-slate-300 bg-white/80 px-4 py-3 text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 disabled:cursor-not-allowed disabled:opacity-60"
                (input)="limpiarError()"
              />
              @if (mostrarError(username)) {
                <p class="mt-1.5 text-sm text-red-600">
                  {{ username.hasError('required') ? 'El usuario es obligatorio.' : '' }}
                </p>
              }
            </div>

            <div>
              <label for="password" class="mb-1.5 block text-sm font-medium text-slate-700">
                Contraseña
              </label>
              <div class="relative">
                <input
                  id="password"
                  [type]="hidePassword() ? 'password' : 'text'"
                  formControlName="password"
                  placeholder="••••••••"
                  autocomplete="current-password"
                  class="w-full rounded-xl border border-slate-300 bg-white/80 px-4 py-3 pr-12 text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 disabled:cursor-not-allowed disabled:opacity-60"
                  (input)="limpiarError()"
                />
                <button
                  type="button"
                  class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 transition hover:text-slate-600 focus:outline-none focus:ring-2 focus:ring-blue-500/40"
                  [attr.aria-label]="hidePassword() ? 'Mostrar contraseña' : 'Ocultar contraseña'"
                  [attr.aria-pressed]="!hidePassword()"
                  (click)="togglePassword()"
                >
                  @if (hidePassword()) {
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      class="h-5 w-5"
                      aria-hidden="true"
                    >
                      <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  } @else {
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      class="h-5 w-5"
                      aria-hidden="true"
                    >
                      <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" />
                      <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c6.5 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68" />
                      <path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3.5 7 10 7a9.74 9.74 0 0 0 5.39-1.61" />
                      <line x1="2" x2="22" y1="2" y2="22" />
                    </svg>
                  }
                </button>
              </div>
              @if (mostrarError(password)) {
                <p class="mt-1.5 text-sm text-red-600">
                  {{ password.hasError('required') ? 'La contraseña es obligatoria.' : '' }}
                </p>
              }
            </div>

            @if (error()) {
              <app-alert kind="error" title="No se pudo iniciar sesión" [message]="error()!" />
            }

            <button
              type="submit"
              [disabled]="isLoading()"
              class="w-full rounded-xl bg-blue-600 px-4 py-3 font-semibold text-white transition hover:bg-blue-700 focus:outline-none focus:ring-4 focus:ring-blue-500/30 disabled:cursor-not-allowed disabled:opacity-60"
            >
              @if (isLoading()) {
                <span>Iniciando sesión...</span>
              } @else {
                <span>Iniciar sesión</span>
              }
            </button>
          </form>
        </div>
      </div>
    </div>
  `,
})
export class LoginPage {
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
  protected readonly hidePassword = signal(true);
  protected readonly isLoading = signal(false);
  protected readonly error = signal<string | null>(null);
  /** true tras el primer intento de envío: activa la validación visible. */
  protected readonly submitted = signal(false);

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** El error solo se muestra tras tocar el campo o intentar enviar. */
  protected mostrarError(campo: FormControl<string>): boolean {
    return campo.invalid && (campo.touched || this.submitted());
  }

  protected togglePassword(): void {
    this.hidePassword.update((v) => !v);
  }

  protected limpiarError(): void {
    this.error.set(null);
  }

  protected enviar(): void {
    this.error.set(null);
    this.submitted.set(true);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    // Angular recomienda deshabilitar vía el control (no con [disabled] en el
    // template): así el atributo se refleja en el DOM sin errores de CD.
    this.form.disable();
    this.auth
      .login({ username: this.username.value, password: this.password.value })
      .subscribe({
        next: () => this.router.navigate(['/materiales']),
        error: (err) => {
          this.isLoading.set(false);
          this.form.enable();
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
