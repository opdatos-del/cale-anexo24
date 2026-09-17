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
    <div class="mx-auto max-w-[1440px] px-6 py-5">
      <div class="mb-6">
        <h1 class="m-0 text-xl font-normal text-slate-800">Hola, {{ auth.userName() || 'Usuario' }}</h1>
        <p class="mt-1 text-sm text-slate-500">Resumen general del sistema</p>
      </div>

      <section class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4" aria-label="Resumen del sistema">
        <mat-card class="dashboard-card !rounded-[28px] !border !border-slate-200 !bg-white !p-4 !shadow-sm">
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
        <mat-card class="dashboard-card !rounded-[28px] !border !border-slate-200 !bg-white !p-4 !shadow-sm">
          <div class="flex items-center gap-3"><span class="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-50 text-emerald-700"><mat-icon class="!text-[20px]">arrow_downward</mat-icon></span><p class="m-0 text-sm text-slate-700">Entradas</p></div>
          <p class="mt-4 text-sm text-blue-700">Consulta pendiente de endpoint</p>
        </mat-card>
        <mat-card class="dashboard-card !rounded-[28px] !border !border-slate-200 !bg-white !p-4 !shadow-sm">
          <div class="flex items-center gap-3"><span class="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-50 text-amber-700"><mat-icon class="!text-[20px]">arrow_upward</mat-icon></span><p class="m-0 text-sm text-slate-700">Salidas</p></div>
          <p class="mt-4 text-sm text-blue-700">Consulta pendiente de endpoint</p>
        </mat-card>
        <mat-card class="dashboard-card !rounded-[28px] !border !border-slate-200 !bg-white !p-4 !shadow-sm">
          <div class="flex items-center gap-3"><span class="flex h-9 w-9 items-center justify-center rounded-xl bg-violet-50 text-violet-700"><mat-icon class="!text-[20px]">bar_chart</mat-icon></span><p class="m-0 text-sm text-slate-700">Reportes</p></div>
          <p class="mt-4 text-sm text-blue-700">Disponibles según permisos</p>
        </mat-card>
      </section>

      <section class="mt-8" aria-labelledby="avisos-title">
        <h2 id="avisos-title" class="mb-3 text-sm font-normal uppercase text-slate-700">Avisos</h2>
        <div class="flex items-center gap-2 border border-slate-300 bg-slate-100 px-4 py-3 text-sm text-slate-600">
          <mat-icon class="!h-5 !w-5 !text-[20px] text-blue-700">info</mat-icon>
          <span>No hay avisos disponibles.</span>
        </div>
      </section>

      <section class="mt-8" aria-labelledby="estado-title">
        <h2 id="estado-title" class="mb-3 text-sm font-normal uppercase text-slate-700">Estado</h2>
        <div class="flex flex-wrap gap-4">
          <div class="min-w-32 rounded-lg border border-slate-400 bg-emerald-100 px-5 py-3 text-center text-sm text-slate-700">Sesión activa</div>
          <div class="min-w-32 rounded-lg border border-slate-300 bg-slate-100 px-5 py-3 text-center text-sm text-slate-600">API pendiente</div>
          <div class="min-w-32 rounded-lg border border-slate-300 bg-slate-100 px-5 py-3 text-center text-sm text-slate-600">BD pendiente</div>
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
