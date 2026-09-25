import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { of } from 'rxjs';
import { NotificationService } from '@core/notifications/notification.service';
import { SearchAuditLogUseCase } from '@features/administration/audit-log/application/use-cases/search-audit-log.use-case';
import { AuditLogListPage } from './audit-log-list.page';

describe('AuditLogListPage', () => {
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
