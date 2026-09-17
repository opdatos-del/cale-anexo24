import { Component, OnInit, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../../core/auth/auth.service';
import { DashboardSummaryService } from '../../../application/dashboard-summary.service';

/** Página de inicio autenticada, alineada al mockup DASH. */
@Component({
  imports: [MatCardModule, MatIconModule, RouterLink],
  selector: 'app-dashboard',
  styleUrl: './dashboard.page.scss',
  template: `
    <div class="mx-auto max-w-[1440px] px-5 py-8 sm:px-8">
      <div class="mb-8">
        <p class="mb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Panel principal</p>
        <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Hola, {{ auth.userName() || 'Usuario' }}</h1>
        <p class="mt-2 text-sm text-slate-500">Resumen operativo de tu control de inventarios.</p>
      </div>

      <section class="welcome-notice mb-7" aria-label="Mensaje de bienvenida">
        <span class="welcome-icon"><mat-icon>verified</mat-icon></span>
        <div>
          <p>Bienvenido de nuevo, {{ auth.userName() || 'Usuario' }}.</p>
          <span>Tu sesión está activa y el sistema está listo para operar.</span>
        </div>
      </section>

      <section class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4" aria-label="Resumen del sistema">
        <mat-card class="dashboard-card !rounded-2xl !border !border-slate-200/80 !bg-white !p-5 !shadow-[0_4px_18px_rgb(15_23_42_/_4%)]">
          <div class="flex items-center gap-3">
            <span class="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-50 text-blue-700"><mat-icon class="!text-[20px]">inventory_2</mat-icon></span>
            <p class="m-0 text-sm text-slate-700">Materiales</p>
          </div>
          @if (isLoading()) {
            <p class="mt-4 text-sm text-slate-400">Cargando…</p>
          } @else if (materialsTotal() !== null) {
            <a routerLink="/materiales" class="mt-4 inline-block text-base text-blue-700 hover:underline">{{ materialsTotal() }} items</a>
          } @else {
            <p class="mt-4 text-sm text-slate-500">No disponible</p>
          }
        </mat-card>
        <mat-card class="dashboard-card !rounded-2xl !border !border-slate-200/80 !bg-white !p-5 !shadow-[0_4px_18px_rgb(15_23_42_/_4%)]">
          <div class="flex items-center gap-3"><span class="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-50 text-emerald-700"><mat-icon class="!text-[20px]">arrow_downward</mat-icon></span><p class="m-0 text-sm text-slate-700">Entradas</p></div>
          <p class="mt-4 text-sm text-blue-700">Consulta pendiente de endpoint</p>
        </mat-card>
        <mat-card class="dashboard-card !rounded-2xl !border !border-slate-200/80 !bg-white !p-5 !shadow-[0_4px_18px_rgb(15_23_42_/_4%)]">
          <div class="flex items-center gap-3"><span class="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-50 text-amber-700"><mat-icon class="!text-[20px]">arrow_upward</mat-icon></span><p class="m-0 text-sm text-slate-700">Salidas</p></div>
          <p class="mt-4 text-sm text-blue-700">Consulta pendiente de endpoint</p>
        </mat-card>
        <mat-card class="dashboard-card !rounded-2xl !border !border-slate-200/80 !bg-white !p-5 !shadow-[0_4px_18px_rgb(15_23_42_/_4%)]">
          <div class="flex items-center gap-3"><span class="flex h-9 w-9 items-center justify-center rounded-xl bg-violet-50 text-violet-700"><mat-icon class="!text-[20px]">bar_chart</mat-icon></span><p class="m-0 text-sm text-slate-700">Reportes</p></div>
          <p class="mt-4 text-sm text-blue-700">Disponibles según permisos</p>
        </mat-card>
      </section>

      <section class="mt-8" aria-labelledby="avisos-title">
        <h2 id="avisos-title" class="mb-3 text-sm font-normal uppercase text-slate-700">Avisos</h2>
        <div class="flex items-center gap-3 rounded-xl border border-blue-100 bg-blue-50/70 px-4 py-3 text-sm text-slate-600">
          <mat-icon class="!h-5 !w-5 !text-[20px] text-blue-700">info</mat-icon>
          <span>No hay avisos operativos pendientes.</span>
        </div>
      </section>

      <section class="mt-8" aria-labelledby="estado-title">
        <h2 id="estado-title" class="mb-3 text-sm font-normal uppercase text-slate-700">Estado</h2>
        <div class="flex flex-wrap gap-4">
          <div class="min-w-32 rounded-xl border border-emerald-200 bg-emerald-50 px-5 py-3 text-center text-sm font-medium text-emerald-800">Sesión activa</div>
          <div class="min-w-32 rounded-xl border border-slate-200 bg-white px-5 py-3 text-center text-sm text-slate-500">API pendiente</div>
          <div class="min-w-32 rounded-xl border border-slate-200 bg-white px-5 py-3 text-center text-sm text-slate-500">BD pendiente</div>
        </div>
      </section>
    </div>
  `,
})
export class DashboardPage implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly materialsTotal = signal<number | null>(null);
  protected readonly isLoading = signal(true);

  private readonly service = inject(DashboardSummaryService);

  ngOnInit(): void {
    this.service.resumen().subscribe({
      next: (resumen) => {
        this.materialsTotal.set(resumen.materialsTotal);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }
}
