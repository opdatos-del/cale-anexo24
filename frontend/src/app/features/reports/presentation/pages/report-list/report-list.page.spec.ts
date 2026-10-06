import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notifications/notification.service';
import { ExportReportUseCase } from '@features/reports/application/use-cases/export-report.use-case';
import { SearchReportUseCase } from '@features/reports/application/use-cases/search-report.use-case';
import { ReportPage, ReportRow } from '@features/reports/domain/models/report.model';
import { ReportListPage } from './report-list.page';

interface PageInternals {
  selectedType: { set(value: string): void };
  generate(): void;
  hasGenerated(): boolean;
}

const fila: ReportRow = {
  pedimentoEntrada: '6001720',
  partidaEntrada: '2.0',
  pedimentoSalida: '190-3302-6002960',
  partidaSalida: '4.0',
  material: '500005',
  producto: '301277',
  fechaSalida: '2026-08-18T00:00:00',
  fechaVencimiento: '2027-05-28T00:00:00',
  cantidadIncorporada: '4.5',
  cantidadMerma: '4.0',
  cantidadDesperdicio: null,
  unidad: 'KG',
};

const pagina: ReportPage = { items: [fila], total: 1, page: 1, pageSize: 20 };

describe('ReportListPage (formato de partidas)', () => {
  let fixture: ComponentFixture<ReportListPage>;
  const search = { execute: vi.fn() };
  const exportReport = { execute: vi.fn() };

  function celdas(): HTMLElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('tbody tr td')) as HTMLElement[];
  }

  beforeEach(() => {
    search.execute.mockReset().mockReturnValue(of(pagina));
    exportReport.execute.mockReset();
    TestBed.configureTestingModule({
      imports: [ReportListPage],
      providers: [
        provideNoopAnimations(),
        { provide: AuthService, useValue: { hasPermission: () => true, hasAnyPermission: () => true } },
        { provide: SearchReportUseCase, useValue: search },
        { provide: ExportReportUseCase, useValue: exportReport },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn(), info: vi.fn() } },
      ],
    });
    fixture = TestBed.createComponent(ReportListPage);
    fixture.detectChanges();
  });

  it('muestra las partidas sin sufijo .0 y conserva las cantidades', () => {
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('analisis-descargas');
    fixture.detectChanges();

    internals.generate();
    fixture.detectChanges();

    expect(internals.hasGenerated()).toBe(true);
    expect(search.execute).toHaveBeenCalledWith(expect.objectContaining({ type: 'analisis-descargas', page: 1, pageSize: 20 }));

    const cells = celdas();
    // Orden de columnas: pedimentoEntrada, partidaEntrada, pedimentoSalida, partidaSalida,
    // material, producto, fechaSalida, fechaVencimiento, cantidadIncorporada, cantidadMerma, ...
    expect(cells[1]?.textContent?.trim()).toBe('2');
    expect(cells[3]?.textContent?.trim()).toBe('4');
    // Las cantidades siguen su propio formatter (no pasan por el de partida).
    expect(cells[8]?.textContent?.trim()).toBe('4.5');
    expect(cells[9]?.textContent?.trim()).toBe('4.0');
  });

  it('habilita XLSX para el análisis de descargas generado', () => {
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('analisis-descargas');
    fixture.detectChanges();

    internals.generate();
    fixture.detectChanges();

    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    expect(buttons.some((button) => button.textContent?.includes('XLSX'))).toBe(true);
  });

  it('habilita XLSX para dirigidos generados', () => {
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('dirigidos');
    fixture.detectChanges();

    internals.generate();
    fixture.detectChanges();

    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    expect(buttons.some((button) => button.textContent?.includes('XLSX'))).toBe(true);
  });

  it('no altera partidas no numéricas ni valores ausentes', () => {
    search.execute.mockReturnValue(of({ ...pagina, items: [{ ...fila, partidaEntrada: 'A4', partidaSalida: null }] }));
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('analisis-descargas');
    fixture.detectChanges();

    internals.generate();
    fixture.detectChanges();

    const cells = celdas();
    expect(cells[1]?.textContent?.trim()).toBe('A4');
    expect(cells[3]?.textContent?.trim()).toBe('—');
  });
});
