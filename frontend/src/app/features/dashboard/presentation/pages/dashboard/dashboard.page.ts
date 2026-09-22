import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import { DashboardSummaryService } from '../../../application/dashboard-summary.service';
import { NotificationService } from '@core/notifications/notification.service';
import { userFacingApiError } from '@core/http/api-error.util';
import { AppAlertComponent } from '@core/ui/app-alert/app-alert.component';

/** Página de inicio autenticada, alineada al mockup DASH. */
@Component({
  imports: [AppAlertComponent, MatButtonModule, MatCardModule, MatIconModule, RouterLink],
  selector: 'app-dashboard',
  styleUrl: './dashboard.page.scss',
  template: `
    <div class="mx-auto max-w-360 px-5 py-8 sm:px-8">
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

      @if (loadError()) {
        <div class="mb-7">
          <app-alert kind="error" title="No pudimos actualizar el resumen" [message]="loadError()!" actionLabel="Reintentar" (action)="loadSummary()" />
        </div>
      }

      @if (auth.hasAnyPermission('MATERIALES_CONSULTAR', 'PRODUCTOS_CONSULTAR', 'ESTRUCTURAS_CONSULTAR', 'OPERACIONES_CONSULTAR')) {
        <section class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3" aria-label="Accesos rápidos de catálogos">
        @if (auth.hasPermission('MATERIALES_CONSULTAR')) {
          <mat-card class="dashboard-card rounded-2xl! border! border-slate-200/80! bg-white! p-5! shadow-[0_4px_18px_rgb(15_23_42/4%)]!">
            <div class="flex items-center gap-3">
              <span class="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-50 text-blue-700"><mat-icon class="text-[20px]!">inventory_2</mat-icon></span>
              <p class="m-0 text-sm text-slate-700">Materiales</p>
            </div>
            @if (isLoading()) {
              <div class="mt-4 h-7 w-24 animate-pulse rounded bg-slate-100" aria-label="Cargando materiales"></div>
            } @else if (materialsTotal() !== null) {
              <a routerLink="/materiales" class="mt-4 inline-block text-base font-medium text-blue-700 hover:underline focus:outline-none focus:ring-2 focus:ring-blue-500">{{ materialsTotal() }} registros</a>
            }
          </mat-card>
        }
        @if (auth.hasPermission('PRODUCTOS_CONSULTAR')) {
          <a routerLink="/productos" class="dashboard-card rounded-2xl border border-slate-200/80 bg-white p-5 shadow-[0_4px_18px_rgb(15_23_42/4%)] focus:outline-none focus:ring-2 focus:ring-blue-500">
            <div class="flex items-center gap-3">
              <span class="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-50 text-indigo-700"><mat-icon class="text-[20px]!">category</mat-icon></span>
              <p class="m-0 text-sm text-slate-700">Productos</p>
            </div>
            <p class="mt-4 mb-0 text-sm leading-6 text-slate-500">Consulta del catálogo de productos terminados.</p>
          </a>
        }
        @if (auth.hasPermission('ESTRUCTURAS_CONSULTAR')) {
          <a routerLink="/estructuras" class="dashboard-card rounded-2xl border border-slate-200/80 bg-white p-5 shadow-[0_4px_18px_rgb(15_23_42/4%)] focus:outline-none focus:ring-2 focus:ring-blue-500">
            <div class="flex items-center gap-3">
              <span class="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-50 text-cyan-700"><mat-icon class="text-[20px]!">account_tree</mat-icon></span>
              <p class="m-0 text-sm text-slate-700">Estructuras</p>
            </div>
            <p class="mt-4 mb-0 text-sm leading-6 text-slate-500">Consulta de estructuras y materiales asociados.</p>
          </a>
        }
        @if (auth.hasPermission('OPERACIONES_CONSULTAR')) {
          <a routerLink="/operaciones/entradas" class="dashboard-card rounded-2xl border border-slate-200/80 bg-white p-5 shadow-[0_4px_18px_rgb(15_23_42/4%)] focus:outline-none focus:ring-2 focus:ring-blue-500">
            <div class="flex items-center gap-3">
              <span class="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-50 text-emerald-700"><mat-icon class="text-[20px]!">move_to_inbox</mat-icon></span>
              <p class="m-0 text-sm text-slate-700">Entradas</p>
            </div>
            <p class="mt-4 mb-0 text-sm leading-6 text-slate-500">Consulta de líneas de importación.</p>
          </a>
          <a routerLink="/operaciones/salidas" class="dashboard-card rounded-2xl border border-slate-200/80 bg-white p-5 shadow-[0_4px_18px_rgb(15_23_42/4%)] focus:outline-none focus:ring-2 focus:ring-blue-500">
            <div class="flex items-center gap-3">
              <span class="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-50 text-amber-700"><mat-icon class="text-[20px]!">outbox</mat-icon></span>
              <p class="m-0 text-sm text-slate-700">Salidas</p>
            </div>
            <p class="mt-4 mb-0 text-sm leading-6 text-slate-500">Consulta de líneas de exportación.</p>
          </a>
          <a routerLink="/operaciones/materiales-utilizados" class="dashboard-card rounded-2xl border border-slate-200/80 bg-white p-5 shadow-[0_4px_18px_rgb(15_23_42/4%)] focus:outline-none focus:ring-2 focus:ring-blue-500">
            <div class="flex items-center gap-3">
              <span class="flex h-9 w-9 items-center justify-center rounded-xl bg-violet-50 text-violet-700"><mat-icon class="text-[20px]!">layers</mat-icon></span>
              <p class="m-0 text-sm text-slate-700">Materiales utilizados</p>
            </div>
            <p class="mt-4 mb-0 text-sm leading-6 text-slate-500">Consulta del histórico de materiales consumidos.</p>
          </a>
          <a routerLink="/operaciones/activos-fijos" class="dashboard-card rounded-2xl border border-slate-200/80 bg-white p-5 shadow-[0_4px_18px_rgb(15_23_42/4%)] focus:outline-none focus:ring-2 focus:ring-blue-500">
            <div class="flex items-center gap-3">
              <span class="flex h-9 w-9 items-center justify-center rounded-xl bg-rose-50 text-rose-700"><mat-icon class="text-[20px]!">precision_manufacturing</mat-icon></span>
              <p class="m-0 text-sm text-slate-700">Activos fijos</p>
            </div>
            <p class="mt-4 mb-0 text-sm leading-6 text-slate-500">Consulta de partidas de importación marcadas como activos fijos.</p>
          </a>
        }
        </section>
      } @else {
        <section class="mb-7" aria-label="Módulos disponibles">
          <app-alert
            kind="info"
            title="Sin módulos asignados"
            message="Tu perfil no tiene módulos asignados. Contacta al administrador para solicitar acceso."
          />
        </section>
      }

      <section class="mt-8" aria-labelledby="avisos-title">
        <h2 id="avisos-title" class="mb-3 text-sm font-normal uppercase text-slate-700">Avisos</h2>
        <app-alert kind="info" message="No hay avisos operativos pendientes." />
      </section>

      <section class="mt-8" aria-labelledby="estado-title">
        <h2 id="estado-title" class="mb-3 text-sm font-normal uppercase text-slate-700">Estado</h2>
      <div class="flex flex-wrap gap-4">
          <div class="flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-5 py-3 text-sm font-medium text-emerald-800">
            <span class="h-2 w-2 rounded-full bg-emerald-500" aria-hidden="true"></span>
            Sesión activa
          </div>
          <div class="rounded-xl border border-slate-200 bg-white px-5 py-3 text-sm text-slate-500">Catálogo conectado</div>
        </div>
      </section>
    </div>
  `,
})
export class DashboardPage implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly materialsTotal = signal<number | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly loadError = signal<string | null>(null);

  private readonly service = inject(DashboardSummaryService);
  private readonly notifications = inject(NotificationService);

  ngOnInit(): void {
    this.loadSummary();
  }

  protected loadSummary(): void {
    if (!this.auth.hasPermission('MATERIALES_CONSULTAR')) {
      this.isLoading.set(false);
      this.loadError.set(null);
      this.materialsTotal.set(null);
      return;
    }

    this.isLoading.set(true);
    this.loadError.set(null);
    this.service.resumen().subscribe({
      next: (resumen) => {
        this.materialsTotal.set(resumen.materialsTotal);
        this.isLoading.set(false);
      },
      error: (error: unknown) => {
        this.isLoading.set(false);
        this.loadError.set(userFacingApiError(error, 'No pudimos actualizar el resumen.'));
        this.notifications.error('No fue posible cargar el resumen del sistema.');
      },
    });
  }
}
