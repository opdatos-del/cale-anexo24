import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Pantalla de inicio de sesión (CU-001).
 *
 * Responsabilidad: capturar credenciales y delegar la autenticación a
 * {@link AuthService}. El componente no ejecuta llamadas HTTP.
 */
@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-login',
  template: `
    <div class="relative min-h-screen w-full overflow-hidden">
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
          class="animate-card-in w-full max-w-[420px] rounded-3xl border border-white/20 bg-white/90 p-8 shadow-2xl backdrop-blur-xl"
        >
          <!-- HEADER -->
          <header class="text-center">
            <div
              class="mx-auto mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-600 shadow-lg"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                class="h-7 w-7 text-white"
                aria-hidden="true"
              >
                <path d="M22 8.35V20a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8.35A2 2 0 0 1 3.26 6.5l8-3.2a2 2 0 0 1 1.48 0l8 3.2A2 2 0 0 1 22 8.35Z" />
                <path d="M6 18h12" />
                <path d="M6 14h12" />
                <rect width="12" height="12" x="6" y="10" />
              </svg>
            </div>
            <h1 class="text-2xl font-semibold tracking-tight text-slate-900">Anexo 24</h1>
            <p class="mt-1 text-sm text-slate-500">Sistema de control aduanero</p>
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
                  [type]="ocultarPassword() ? 'password' : 'text'"
                  formControlName="password"
                  placeholder="••••••••"
                  autocomplete="current-password"
                  class="w-full rounded-xl border border-slate-300 bg-white/80 px-4 py-3 pr-12 text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10 disabled:cursor-not-allowed disabled:opacity-60"
                  (input)="limpiarError()"
                />
                <button
                  type="button"
                  class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 transition hover:text-slate-600 focus:outline-none focus:ring-2 focus:ring-blue-500/40"
                  [attr.aria-label]="ocultarPassword() ? 'Mostrar contraseña' : 'Ocultar contraseña'"
                  [attr.aria-pressed]="!ocultarPassword()"
                  (click)="alternarPassword()"
                >
                  @if (ocultarPassword()) {
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
              <p
                role="alert"
                class="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  class="h-4 w-4 shrink-0"
                  aria-hidden="true"
                >
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" x2="12" y1="8" y2="12" />
                  <line x1="12" x2="12.01" y1="16" y2="16" />
                </svg>
                <span>{{ error() }}</span>
              </p>
            }

            <button
              type="submit"
              [disabled]="cargando()"
              class="w-full rounded-xl bg-blue-600 px-4 py-3 font-semibold text-white transition hover:bg-blue-700 focus:outline-none focus:ring-4 focus:ring-blue-500/30 disabled:cursor-not-allowed disabled:opacity-60"
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
    // Angular recomienda deshabilitar vía el control (no con [disabled] en el
    // template): así el atributo se refleja en el DOM sin errores de CD.
    this.form.disable();
    this.auth
      .login({ clave: this.username.value, password: this.password.value })
      .subscribe({
        next: () => this.router.navigate(['/materiales']),
        error: (err) => {
          this.cargando.set(false);
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