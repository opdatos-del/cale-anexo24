import { Component, OnInit, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../../core/auth/auth.service';
import { NotificationService } from '../../../../../core/notifications/notification.service';
import { AppAlertComponent } from '../../../../../core/ui/app-alert/app-alert.component';
import { AppPanelComponent } from '../../../../../core/ui/app-panel/app-panel.component';
import { PageHeaderComponent } from '../../../../../core/ui/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../../../core/ui/status-badge/status-badge.component';
import { DashboardSummaryService } from '../../../application/dashboard-summary.service';

/** Página de inicio autenticada con indicadores respaldados por endpoints reales. */
@Component({
  imports: [
    AppAlertComponent,
    AppPanelComponent,
    MatIconModule,
    PageHeaderComponent,
    RouterLink,
    StatusBadgeComponent,
  ],
  selector: 'app-dashboard',
  styleUrl: './dashboard.page.scss',
  template: `
    <div class="app-page">
      <main class="app-page__content dashboard-page">
        <app-page-header
          eyebrow="Panel principal"
          [title]="'Hola, ' + (auth.userName() || 'Usuario')"
          description="Un punto de entrada claro para consultar y controlar tus catálogos."
        />

        <section class="dashboard-welcome" aria-label="Estado de la sesión">
          <span class="dashboard-welcome__icon" aria-hidden="true"><mat-icon>verified_user</mat-icon></span>
          <div>
            <p>Tu sesión está activa.</p>
            <span>El sistema está listo para consultar la información disponible.</span>
          </div>
        </section>

        @if (loadError()) {
          <div class="dashboard-alert">
            <app-alert kind="error" title="No pudimos actualizar el resumen" [message]="loadError()!" actionLabel="Reintentar" (action)="loadSummary()" />
          </div>
        }

        <section class="dashboard-section" aria-labelledby="catalogos-title">
          <div class="dashboard-section__heading">
            <div>
              <p class="dashboard-section__eyebrow">Acceso operativo</p>
              <h2 id="catalogos-title">Catálogos</h2>
            </div>
            <span class="dashboard-section__hint">Información disponible</span>
          </div>

          <div class="dashboard-grid">
            <app-panel class="dashboard-module-card">
              <div class="dashboard-module-card__header">
                <span class="dashboard-module-card__icon" aria-hidden="true"><mat-icon>inventory_2</mat-icon></span>
                <div>
                  <h3>Materiales</h3>
                  <p>Consulta el catálogo registrado en Módulo C.</p>
                </div>
              </div>

              @if (isLoading()) {
                <div class="dashboard-stat-skeleton" aria-label="Cargando total de materiales"></div>
              } @else if (materialsTotal() !== null) {
                <div class="dashboard-module-card__metric">
                  <strong>{{ materialsTotal() }}</strong>
                  <span>registros disponibles</span>
                </div>
              }

              <a routerLink="/materiales" class="dashboard-module-card__link">
                Consultar catálogo
                <mat-icon aria-hidden="true">arrow_forward</mat-icon>
              </a>
            </app-panel>
          </div>
        </section>

        <section class="dashboard-section" aria-labelledby="system-status-title">
          <div class="dashboard-section__heading">
            <div>
              <p class="dashboard-section__eyebrow">Supervisión</p>
              <h2 id="system-status-title">Estado del sistema</h2>
            </div>
          </div>
          <app-panel class="dashboard-status-panel">
            <div class="dashboard-status-list">
              <app-status-badge kind="success" label="Sesión activa" />
              @if (materialsTotal() !== null) {
                <app-status-badge kind="info" label="Consulta de materiales disponible" />
              } @else {
                <app-status-badge kind="neutral" label="Verificando catálogo" />
              }
            </div>
            <p class="dashboard-status-panel__copy">Los indicadores se muestran únicamente cuando la API confirma la disponibilidad de la información.</p>
          </app-panel>
        </section>
      </main>
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
    this.isLoading.set(true);
    this.loadError.set(null);
    this.service.resumen().subscribe({
      next: (resumen) => {
        this.materialsTotal.set(resumen.materialsTotal);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.loadError.set('No pudimos actualizar el resumen.');
        this.notifications.error('No fue posible cargar el resumen del sistema.');
      },
    });
  }
}
