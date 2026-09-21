import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

/** Pantalla segura para rutas que requieren un permiso no disponible. */
@Component({
  imports: [MatButtonModule, MatIconModule, RouterLink],
  selector: 'app-forbidden',
  template: `
    <main class="flex min-h-[100dvh] items-center justify-center bg-[#f4f7fb] px-5 py-10">
      <section class="w-full max-w-md rounded-3xl border border-slate-200/80 bg-white p-8 text-center shadow-[0_14px_40px_rgb(15_23_42/8%)]" aria-labelledby="forbidden-title">
        <div class="mx-auto mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-50 text-amber-700">
          <mat-icon aria-hidden="true">lock</mat-icon>
        </div>
        <p class="mb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Acceso restringido</p>
        <h1 id="forbidden-title" class="m-0 text-2xl font-semibold tracking-tight text-slate-900">No tiene permiso para acceder a este módulo</h1>
        <p class="mt-3 text-sm leading-6 text-slate-500">Solicita el permiso correspondiente al administrador de tu cuenta.</p>
        <a routerLink="/dashboard" mat-flat-button color="primary" class="mt-7 rounded-xl!">Volver al inicio</a>
      </section>
    </main>
  `,
})
export class ForbiddenComponent {}
