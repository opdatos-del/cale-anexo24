import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notifications/notification.service';
import { ExportReportUseCase } from '@features/reports/application/use-cases/export-report.use-case';
import { SearchReportUseCase } from '@features/reports/application/use-cases/search-report.use-case';
import { ReportListPage } from './report-list.page';

describe('ReportListPage inventario inicial Anexo 30', () => {
  it('muestra contrato read-only y genera consulta textual paginada', () => {
    const search = { execute: vi.fn(() => of({ items: [{ patente: '1234', numeroPedimento: '0000001', fraccion: '84715002', valorComercialHistorico: 100.25 }], total: 1, page: 1, pageSize: 20 })) };
    TestBed.configureTestingModule({
      imports: [ReportListPage],
      providers: [provideNoopAnimations(), { provide: AuthService, useValue: { hasPermission: () => true, hasAnyPermission: () => true } }, { provide: SearchReportUseCase, useValue: search }, { provide: ExportReportUseCase, useValue: { execute: vi.fn() } }, { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn(), info: vi.fn() } }],
    });
    const fixture = TestBed.createComponent(ReportListPage);
    fixture.detectChanges();
    const page = fixture.componentInstance as unknown as { selectedType: { set(type: string): void }; generate(): void };
    page.selectedType.set('anexo30-revision-inventario-inicial');
    fixture.detectChanges();
    page.generate();
    fixture.detectChanges();

    expect(search.execute).toHaveBeenCalledWith(expect.objectContaining({ type: 'anexo30-revision-inventario-inicial', page: 1, pageSize: 20 }));
    expect(fixture.nativeElement.textContent).toContain('Inventario inicial agrupado');
    expect(fixture.nativeElement.textContent).toContain('0000001');
    expect(fixture.nativeElement.querySelector('app-operation-period-filter')).toBeNull();
  });
});
