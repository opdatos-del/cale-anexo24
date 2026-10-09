import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { of } from 'rxjs';
import { NotificationService } from '@core/notifications/notification.service';
import { SearchAuditLogUseCase } from '@features/administration/audit-log/application/use-cases/search-audit-log.use-case';
import { AuditLogListPage } from './audit-log-list.page';

describe('AuditLogListPage', () => {
  it('exporta todos los eventos con criterio actual y conserva fecha ISO cruda y página visible', async () => {
    vi.useFakeTimers();
    const date = '2026-03-04T18:20:30.000Z';
    const entry = { id: 99, date, userId: null, user: null, module: 'SEGURIDAD', action: 'LOGIN_OK', detail: null, result: 'EXITO', correlationId: null };
    const execute = vi.fn(() => of({ items: [entry], total: 1, page: 1, pageSize: 100 }));
    const notifications = { info: vi.fn(), error: vi.fn() };
    TestBed.configureTestingModule({
      imports: [AuditLogListPage],
      providers: [provideNoopAnimations(), { provide: SearchAuditLogUseCase, useValue: { execute } }, { provide: NotificationService, useValue: notifications }],
    });
    const fixture = TestBed.createComponent(AuditLogListPage);
    fixture.detectChanges();
    const page = fixture.componentInstance as unknown as {
      fromDate: Date | null; fromTime: string; toDate: Date | null; toTime: string;
      userIdFilter: number | null; moduleFilter: string; resultFilter: string; correlationFilter: string;
      currentPage: number; pageSize: number; hasSearched: { set(value: boolean): void };
      items: { set(value: typeof entry[]): void; (): typeof entry[] }; totalItems: { set(value: number): void };
      exportCsv(): Promise<void>;
    };
    page.fromDate = new Date(2026, 2, 4); page.fromTime = '18:00:00';
    page.toDate = new Date(2026, 2, 4); page.toTime = '19:00:00';
    page.userIdFilter = 8; page.moduleFilter = 'SEGURIDAD'; page.resultFilter = 'EXITO'; page.correlationFilter = ' corr ';
    page.currentPage = 3; page.pageSize = 50; page.hasSearched.set(true); page.items.set([entry]); page.totalItems.set(1);
    const visibleItems = page.items();
    let exported: Blob | undefined;
    vi.stubGlobal('URL', { createObjectURL: vi.fn((blob: Blob) => { exported = blob; return 'blob:audit'; }), revokeObjectURL: vi.fn() });
    const download = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    await page.exportCsv();

    expect(execute).toHaveBeenLastCalledWith(expect.objectContaining({
      from: new Date(2026, 2, 4, 18, 0, 0).toISOString(),
      to: new Date(2026, 2, 4, 19, 0, 0, 999).toISOString(),
      userId: 8, module: 'SEGURIDAD', result: 'EXITO', correlationId: 'corr', page: 1, pageSize: 100,
    }));
    const csv = await (exported as Blob).text();
    expect(csv).toContain(date);
    expect(csv).toContain('Fecha,Usuario ID,Usuario,Módulo,Acción,Resultado,Detalle,Correlation ID');
    expect(csv).not.toContain('99');
    expect(csv).toContain(',,SEGURIDAD,LOGIN_OK,EXITO,,');
    expect(page.currentPage).toBe(3);
    expect(page.pageSize).toBe(50);
    expect(page.items()).toBe(visibleItems);
    expect(download).toHaveBeenCalledOnce();
    vi.unstubAllGlobals();
    vi.useRealTimers();
  });

  it('mantiene accesibles los toggles de fecha', () => {
    TestBed.configureTestingModule({
      imports: [AuditLogListPage],
      providers: [
        provideNoopAnimations(),
        { provide: SearchAuditLogUseCase, useValue: { execute: vi.fn(() => of({ items: [], total: 0, page: 1, pageSize: 20 })) } },
        { provide: NotificationService, useValue: { error: vi.fn() } },
      ],
    });

    const fixture = TestBed.createComponent(AuditLogListPage);
    fixture.detectChanges();

    const toggles = Array.from(fixture.nativeElement.querySelectorAll('mat-datepicker-toggle')) as HTMLElement[];
    expect(toggles).toHaveLength(2);
    expect(toggles.every((toggle) => toggle.getAttribute('aria-hidden') !== 'true')).toBe(true);
  });
});
