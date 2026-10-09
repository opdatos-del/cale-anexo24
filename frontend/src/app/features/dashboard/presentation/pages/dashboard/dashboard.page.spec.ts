import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, expect, it } from 'vitest';
import { of } from 'rxjs';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notifications/notification.service';
import { DashboardSummaryService } from '@features/dashboard/application/dashboard-summary.service';
import { DashboardPage } from './dashboard.page';

function configure(permissions: string[]) {
  TestBed.configureTestingModule({
    imports: [DashboardPage],
    providers: [
      provideRouter([]),
      { provide: AuthService, useValue: {
        userName: () => 'Usuario',
        hasPermission: (permission: string) => permissions.includes(permission),
        hasAnyPermission: (...requested: string[]) => requested.some((permission) => permissions.includes(permission)),
      } },
      { provide: DashboardSummaryService, useValue: { resumen: () => of({ materialsTotal: null, productsTotal: null, structuresTotal: null, system: { status: 'UP', moduleCDatabase: 'UP', applicationDatabase: 'UP', degraded: false } }) } },
      { provide: NotificationService, useValue: { error: () => undefined } },
    ],
  });
  const fixture = TestBed.createComponent(DashboardPage);
  fixture.detectChanges();
  return fixture;
}

describe('DashboardPage quick links', () => {
  it('shows reports with REPORTES_GENERAR', () => {
    const fixture = configure(['REPORTES_GENERAR']);
    expect(fixture.nativeElement.textContent).toContain('Reportes');
  });

  it('hides reports without REPORTES_GENERAR', () => {
    const fixture = configure([]);
    expect(fixture.nativeElement.textContent).not.toContain('Consultas y exportaciones disponibles.');
  });

  it.each(['CLIENTES_CARGAR', 'AGENTES_CARGAR'])('shows catalog imports with %s only', (permission) => {
    const fixture = configure([permission]);
    expect(fixture.nativeElement.textContent).toContain('Importaciones de catálogos');
  });

  it('hides catalog imports without a catalog upload permission', () => {
    const fixture = configure(['REPORTES_GENERAR']);
    expect(fixture.nativeElement.textContent).not.toContain('Validación y previsualización de catálogos.');
  });

  it('shows billing with FACTURACION_CARGAR', () => {
    const fixture = configure(['FACTURACION_CARGAR']);
    expect(fixture.nativeElement.textContent).toContain('Facturación');
    expect(fixture.nativeElement.textContent).toContain('Carga y validación de archivos de facturación.');
  });
});
