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

  it('habilita XLSX para el anÃ¡lisis de descargas generado', () => {
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

  it('no altera partidas no numÃ©ricas ni valores ausentes', () => {
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
  it('presenta fracciones como reporte textual con columnas y copy read-only', () => {
    search.execute.mockReturnValue(of({ items: [{ tipo: 'F4', clavePedimento: 'A1', ejercicio: '2026', periodo: '01', fraccion: '84715002', valor: 10, af: 'SI', archivo: 'a.txt' }], total: 1, page: 1, pageSize: 20 }));
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('anexo30-revision-fracciones'); fixture.detectChanges(); internals.generate(); fixture.detectChanges();
    expect(search.execute).toHaveBeenCalledWith(expect.objectContaining({ type: 'anexo30-revision-fracciones', page: 1, pageSize: 20 }));
    expect(fixture.nativeElement.textContent).toContain('Registros persistidos de fracción utilizados por el proceso Anexo 30.');
    expect(fixture.nativeElement.textContent).toContain('Clave pedimento');
    expect(fixture.nativeElement.textContent).not.toContain('fraccionKey');
    expect(fixture.nativeElement.querySelector('app-operation-period-filter')).toBeNull();
  });

  it('presenta descargas como reporte textual sin fechas ni IDs técnicos', () => {
    search.execute.mockReturnValue(of({ items: [{ pedimento: 'PED-1', fechaEntrada: '2026-01-01T00:00:00', clavePedimentoEntrada: 'A1', partida: '1', esaf: 'SI', fraccionEntrada: '84715002', fraccionDescarga: '84715002', valorComercialEntrada: 10, valorDescargado: 2, ejercicio: '2026', periodo: '01', clavePedimentoA31: 'A1', fraccionA31: '84715002', archivo: 'a.txt' }], total: 1, page: 1, pageSize: 20 }));
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('anexo30-revision-descargas'); fixture.detectChanges(); internals.generate(); fixture.detectChanges();
    expect(search.execute).toHaveBeenCalledWith(expect.objectContaining({ type: 'anexo30-revision-descargas', page: 1, pageSize: 20 }));
    expect(fixture.nativeElement.textContent).toContain('Esta consulta no ejecuta ni recalcula el proceso.');
    expect(fixture.nativeElement.textContent).toContain('Valor descargado');
    expect(fixture.nativeElement.textContent).not.toContain('descargaKey');
    expect(fixture.nativeElement.querySelector('app-operation-period-filter')).toBeNull();
  });

  it('conserva entradas A31 como textual y entradas como reporte con periodo', () => {
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('anexo30-revision-entradas'); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-operation-period-filter')).toBeNull();
    internals.selectedType.set('entradas'); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-operation-period-filter')).not.toBeNull();
  });

});
