import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from './auth.service';

/**
 * Pantalla de inicio de sesión (CU-001).
 */
@Component({
  imports: [FormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule],
  selector: 'app-login',
  template: `
    <div class="flex min-h-screen items-center justify-center bg-gray-100">
      <mat-card class="w-[360px]">
        <mat-card-header>
          <mat-card-title>Acceso Sistema</mat-card-title>
          <mat-card-subtitle>Anexo 24</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form #form="ngForm" (ngSubmit)="enviar()" class="flex flex-col gap-3 pt-3">
            <mat-form-field appearance="outline">
              <mat-label>Clave</mat-label>
              <input matInput name="clave" [(ngModel)]="clave" required autocomplete="username" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Contraseña</mat-label>
              <input
                matInput
                name="password"
                type="password"
                [(ngModel)]="password"
                required
                autocomplete="current-password"
              />
            </mat-form-field>
            @if (error()) {
              <p class="m-0 text-sm text-red-800">{{ error() }}</p>
            }
            <button
              mat-raised-button
              color="primary"
              type="submit"
              [disabled]="form.invalid || cargando()"
              class="w-full"
            >
              {{ cargando() ? 'Ingresando…' : 'Ingresar' }}
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
})
export class LoginComponent {
  protected clave = '';
  protected password = '';
  protected cargando = signal(false);
  protected error = signal<string | null>(null);

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected enviar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.auth.login({ clave: this.clave, password: this.password }).subscribe({
      next: () => this.router.navigate(['/materiales']),
      error: (err) => {
        this.cargando.set(false);
        this.error.set(err.error?.message ?? 'No se pudo iniciar sesión.');
      },
    });
  }
}