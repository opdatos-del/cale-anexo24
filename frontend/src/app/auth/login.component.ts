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
    <div class="login-wrapper">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-card-title>Anexo 24</mat-card-title>
          <mat-card-subtitle>Control automatizado de inventarios</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form #form="ngForm" (ngSubmit)="enviar()">
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
              <p class="error">{{ error() }}</p>
            }
            <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || cargando()">
              {{ cargando() ? 'Ingresando…' : 'Ingresar' }}
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .login-wrapper {
        height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #f5f5f5;
      }
      .login-card {
        width: 360px;
      }
      form {
        display: flex;
        flex-direction: column;
        gap: 12px;
        padding-top: 12px;
      }
      .error {
        color: #b71c1c;
        font-size: 14px;
        margin: 0;
      }
    `,
  ],
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