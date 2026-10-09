import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WritableSignal } from '@angular/core';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notifications/notification.service';
import { ExportReportUseCase } from '@features/reports/application/use-cases/export-report.use-case';
import { SearchReportUseCase } from '@features/reports/application/use-cases/search-report.use-case';
import { ReportPage, ReportRow } from '@features/reports/domain/models/report.model';
import { SavedCriteria } from '@features/saved-queries/domain/saved-query.model';
import { ReportListPage } from './report-list.page';

interface PageInternals {
  selectedType: WritableSignal<string>;
  applySavedCriteria(criteria: SavedCriteria): void;
  fromDate: Date | null;
  toDate: Date | null;
  currentPage: number;
  customsDocument: string;
  generate(): void;
  hasGenerated(): boolean;
  changePage(event: { pageIndex: number; pageSize: number; length: number }): void;
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

  it.each(['anexo30-revision-fracciones', 'anexo30-revision-descargas'] as const)('muestra loading, éxito y vacío para %s', (type) => {
    const pending = new Subject<ReportPage>();
    search.execute.mockReturnValueOnce(pending);
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set(type); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('input[name=filter]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-operation-period-filter')).toBeNull();
    internals.generate(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[aria-label="Generando reporte"]')).not.toBeNull();
    pending.next({ items: [fila], total: 1, page: 1, pageSize: 20 }); pending.complete(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[aria-label="Generando reporte"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('tbody tr')).not.toBeNull();
    search.execute.mockReturnValueOnce(of({ items: [], total: 0, page: 1, pageSize: 20 }));
    internals.generate(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No se encontraron resultados');
  });

  it.each(['anexo30-revision-fracciones', 'anexo30-revision-descargas'] as const)('muestra error y reintenta %s', (type) => {
    search.execute.mockReturnValueOnce(throwError(() => new Error('fallo'))).mockReturnValueOnce(of(pagina));
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set(type); fixture.detectChanges(); internals.generate(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No pudimos generar el reporte');
    const retry = (Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[]).find((button) => button.textContent?.includes('Reintentar')) as HTMLButtonElement;
    retry.click(); fixture.detectChanges();
    expect(search.execute).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).not.toContain('No pudimos generar el reporte');
  });

  it.each(['anexo30-revision-fracciones', 'anexo30-revision-descargas'] as const)('pagina %s conservando filtro técnico', (type) => {
    search.execute.mockReturnValue(of({ items: [fila], total: 45, page: 1, pageSize: 20 }));
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set(type); fixture.detectChanges();
    const filter = fixture.nativeElement.querySelector('input[name=filter]') as HTMLInputElement; filter.value = 'A31'; filter.dispatchEvent(new Event('input'));
    internals.generate(); internals.changePage({ pageIndex: 1, pageSize: 20, length: 45 }); fixture.detectChanges();
    expect(search.execute).toHaveBeenLastCalledWith(expect.objectContaining({ type, filter: 'A31', page: 2, pageSize: 20, from: '', to: '' }));
  });


  it('presenta comparativa como reporte textual sin controles de fecha', () => {
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('anexo30-revision-comparativa'); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-operation-period-filter')).toBeNull();
    expect(fixture.nativeElement.querySelector('input[name=filter]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Revisi\u00f3n Anexo 30 - Comparativa');
  });

  it('muestra help de comparativa con texto exacto', () => {
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('anexo30-revision-comparativa'); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Revisi\u00f3n read-only de la \u00faltima comparativa A31/A24 persistida. La consulta no ejecuta ni recalcula el proceso de comparaci\u00f3n.');
  });

  it.each(['anexo30-revision-comparativa'] as const)('muestra loading, exito, vacio y error para %s', (type) => {
    const pending = new Subject<ReportPage>();
    search.execute.mockReturnValueOnce(pending);
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set(type); fixture.detectChanges();
    internals.generate(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[aria-label="Generando reporte"]')).not.toBeNull();
    pending.next({ items: [fila], total: 1, page: 1, pageSize: 20 }); pending.complete(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('tbody tr')).not.toBeNull();
    search.execute.mockReturnValueOnce(of({ items: [], total: 0, page: 1, pageSize: 20 }));
    internals.generate(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No se encontraron resultados');
  });

  it('comparativa error y reintento', () => {
    search.execute.mockReturnValueOnce(throwError(() => new Error('fallo'))).mockReturnValueOnce(of(pagina));
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('anexo30-revision-comparativa'); fixture.detectChanges(); internals.generate(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No pudimos generar el reporte');
    const retry = (Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[]).find((b) => b.textContent?.includes('Reintentar')) as HTMLButtonElement;
    retry.click(); fixture.detectChanges();
    expect(search.execute).toHaveBeenCalledTimes(2);
  });

  it('comparativa pagina conservando filtro tecnico', () => {
    search.execute.mockReturnValue(of({ items: [fila], total: 45, page: 1, pageSize: 20 }));
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('anexo30-revision-comparativa'); fixture.detectChanges();
    const filter = fixture.nativeElement.querySelector('input[name=filter]') as HTMLInputElement; filter.value = 'AA'; filter.dispatchEvent(new Event('input'));
    internals.generate(); internals.changePage({ pageIndex: 1, pageSize: 20, length: 45 }); fixture.detectChanges();
    expect(search.execute).toHaveBeenLastCalledWith(expect.objectContaining({ type: 'anexo30-revision-comparativa', filter: 'AA', page: 2, pageSize: 20, from: '', to: '' }));
  });

  it.each(['anexo30-revision-entradas', 'anexo30-revision-fracciones', 'anexo30-revision-descargas'] as const)('regresion: %s sigue como text report', (type) => {
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set(type); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-operation-period-filter')).toBeNull();
  });

  it('regresion: entradas sigue siendo reporte con periodo', () => {
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('entradas'); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-operation-period-filter')).not.toBeNull();
  });

  const filaCompulsaDetalle: ReportRow = {
    pedimentoGlosa: 'G-001', secGlosa: 1, pedimentoA24: 'A-001', secA24: 1,
    clavePedimentoGlosa: 'CL-G1', clavePedimentoA24: 'CL-A1', statusClavePedimento: 'OK',
    fechaGlosa: '2026-01-01T00:00:00', fechaA24: '2026-01-02T00:00:00', statusFechas: 'OK',
    fraccionGlosa: '84715001', fraccionA24: '84715001', statusFraccion: 'OK',
    paisOdGlosa: 'MX', paisOdA24: 'MX', statusPaisOd: 'OK', paisCvGlosa: 'US', paisCvA24: 'US', statusPaisCv: 'OK',
    valorAduanaGlosa: 100, valorAduanaA24: 100, statusValorAduana: 'OK',
    valorComercialGlosa: 120, valorComercialA24: 120, statusValorComercial: 'OK',
    cantidadUmcGlosa: 10, cantidadUmcA24: 10, statusCantidadComercial: 'OK',
    cantidadUmtGlosa: 10, cantidadUmtA24: 10, statusCantidadTarifa: 'OK',
    tipoOperacionGlosa: 1, tipoPedimentoGlosa: 1,
  };

  const filaDetalle: ReportRow = {
    pedimento: 'P-0003', clavePedimento: 'A1', descarga: 'D1', pedimentoOriginal: 'P-0001', existePedimento: 'SI',
    clavePedimentoOriginal: 'A1', descargaOriginal: 'D1', status: 'CUIDADO AMBOS DESCARGAN',
  };

  function botones(): HTMLButtonElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
  }

  it('ofrece rectificaciones resumen y detalle como opciones separadas', () => {
    const etiquetas = botones().map((button) => button.textContent ?? '');
    expect(etiquetas.some((text) => text.includes('Rectificaciones - detalle'))).toBe(true);
    expect(etiquetas.some((text) => text.includes('Rectificaciones') && !text.includes('detalle'))).toBe(true);
  });

  it('selecciona rectificaciones detalle como reporte textual sin fechas y con XLSX', () => {
    search.execute.mockReturnValue(of({ items: [filaDetalle], total: 1, page: 1, pageSize: 20 }));
    const opcion = botones().find((button) => button.textContent?.includes('Rectificaciones - detalle')) as HTMLButtonElement;
    opcion.click(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-operation-period-filter')).toBeNull();
    expect(fixture.nativeElement.querySelector('input[name=filter]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Detalle read-only de relaciones de rectificaci\u00f3n persistidas. La consulta no aplica ni procesa rectificaciones.');
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.generate(); fixture.detectChanges();
    expect(search.execute).toHaveBeenCalledWith(expect.objectContaining({ type: 'rectificaciones-detalle', page: 1, pageSize: 20, from: '', to: '' }));
    const texto = fixture.nativeElement.textContent as string;
    for (const columna of ['Pedimento', 'Clave pedimento', 'Descarga', 'Pedimento original', 'Existe pedimento', 'Clave pedimento original', 'Descarga original', 'Estado']) {
      expect(texto).toContain(columna);
    }
    expect(texto).toContain('CUIDADO AMBOS DESCARGAN');
    expect(botones().some((button) => button.textContent?.includes('XLSX'))).toBe(true);
  });

  it('rectificaciones detalle: loading, vacio, error y reintento', () => {
    const pending = new Subject<ReportPage>();
    search.execute.mockReturnValueOnce(pending);
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('rectificaciones-detalle'); fixture.detectChanges();
    internals.generate(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[aria-label="Generando reporte"]')).not.toBeNull();
    pending.next({ items: [filaDetalle], total: 1, page: 1, pageSize: 20 }); pending.complete(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('tbody tr')).not.toBeNull();
    search.execute.mockReturnValueOnce(of({ items: [], total: 0, page: 1, pageSize: 20 }));
    internals.generate(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No se encontraron resultados');
    search.execute.mockReset().mockReturnValueOnce(throwError(() => new Error('fallo'))).mockReturnValueOnce(of(pagina));
    internals.generate(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No pudimos generar el reporte');
    const retry = botones().find((button) => button.textContent?.includes('Reintentar')) as HTMLButtonElement;
    retry.click(); fixture.detectChanges();
    expect(search.execute).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).not.toContain('No pudimos generar el reporte');
  });

  it('rectificaciones detalle pagina conservando el filtro', () => {
    search.execute.mockReturnValue(of({ items: [filaDetalle], total: 45, page: 1, pageSize: 20 }));
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('rectificaciones-detalle'); fixture.detectChanges();
    const filter = fixture.nativeElement.querySelector('input[name=filter]') as HTMLInputElement; filter.value = 'CUIDADO'; filter.dispatchEvent(new Event('input'));
    internals.generate(); internals.changePage({ pageIndex: 1, pageSize: 20, length: 45 }); fixture.detectChanges();
    expect(search.execute).toHaveBeenLastCalledWith(expect.objectContaining({ type: 'rectificaciones-detalle', filter: 'CUIDADO', page: 2, pageSize: 20, from: '', to: '' }));
  });

  it('ofrece compulsa resumen y detalle como opciones separadas', () => {
    const etiquetas = botones().map((button) => button.textContent ?? '');
    expect(etiquetas.some((text) => text.includes('Compulsa - detalle'))).toBe(true);
    expect(etiquetas.some((text) => text.includes('Compulsa') && !text.includes('detalle'))).toBe(true);
  });

  it('selecciona compulsa detalle como reporte textual sin fechas y con XLSX y con 33 columnas', () => {
    search.execute.mockReturnValue(of({ items: [filaCompulsaDetalle], total: 1, page: 1, pageSize: 20 }));
    const opcion = botones().find((button) => button.textContent?.includes('Compulsa - detalle')) as HTMLButtonElement;
    opcion.click(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-operation-period-filter')).toBeNull();
    expect(fixture.nativeElement.querySelector('input[name=filter]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Detalle read-only de la compulsa Glosa vs Anexo 24. Los estados y diferencias provienen de la vista legacy; la consulta no genera ni recalcula la compulsa.');
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.generate(); fixture.detectChanges();
    expect(search.execute).toHaveBeenCalledWith(expect.objectContaining({ type: 'compulsa-detalle', page: 1, pageSize: 20, from: '', to: '' }));
    const texto = fixture.nativeElement.textContent as string;
    const columnas = ['Pedimento Glosa', 'SEC Glosa', 'Pedimento A24', 'SEC A24', 'Clave pedimento Glosa', 'Clave pedimento A24', 'Estado clave', 'Fecha Glosa', 'Fecha A24', 'Estado fechas', 'Fracci\u00f3n Glosa', 'Fracci\u00f3n A24', 'Estado fracci\u00f3n', 'Pa\u00eds OD Glosa', 'Pa\u00eds OD A24', 'Estado pa\u00eds OD', 'Pa\u00eds CV Glosa', 'Pa\u00eds CV A24', 'Estado pa\u00eds CV', 'Valor aduana Glosa', 'Valor aduana A24', 'Estado valor aduana', 'Valor comercial Glosa', 'Valor comercial A24', 'Estado valor comercial', 'Cantidad UMC Glosa', 'Cantidad UMC A24', 'Estado cantidad comercial', 'Cantidad UMT Glosa', 'Cantidad UMT A24', 'Estado cantidad tarifa', 'Tipo operaci\u00f3n Glosa', 'Tipo pedimento Glosa'];
    expect(columnas.every((columna) => texto.includes(columna))).toBe(true);
    expect(botones().some((button) => button.textContent?.includes('XLSX'))).toBe(true);
  });

  it('compulsa detalle muestra loading, exito, vacio, error y reintento', () => {
    const pending = new Subject<ReportPage>();
    search.execute.mockReturnValueOnce(pending);
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('compulsa-detalle'); fixture.detectChanges();
    internals.generate(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[aria-label="Generando reporte"]')).not.toBeNull();
    pending.next({ items: [filaCompulsaDetalle], total: 1, page: 1, pageSize: 20 }); pending.complete(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('tbody tr')).not.toBeNull();
    search.execute.mockReturnValueOnce(of({ items: [], total: 0, page: 1, pageSize: 20 }));
    internals.generate(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No se encontraron resultados');
    search.execute.mockReset().mockReturnValueOnce(throwError(() => new Error('fallo'))).mockReturnValueOnce(of(pagina));
    internals.generate(); fixture.detectChanges();
    const retry = botones().find((button) => button.textContent?.includes('Reintentar')) as HTMLButtonElement;
    retry.click(); fixture.detectChanges();
    expect(search.execute).toHaveBeenCalledTimes(2);
  });

  it('compulsa detalle pagina conservando filtro tecnico y resumen conserva seleccion', () => {
    search.execute.mockReturnValue(of({ items: [filaCompulsaDetalle], total: 45, page: 1, pageSize: 20 }));
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('compulsa-detalle'); fixture.detectChanges();
    const filter = fixture.nativeElement.querySelector('input[name=filter]') as HTMLInputElement; filter.value = 'DIFERENCIA'; filter.dispatchEvent(new Event('input'));
    internals.generate(); internals.changePage({ pageIndex: 1, pageSize: 20, length: 45 }); fixture.detectChanges();
    expect(search.execute).toHaveBeenLastCalledWith(expect.objectContaining({ type: 'compulsa-detalle', filter: 'DIFERENCIA', page: 2, pageSize: 20, from: '', to: '' }));
    internals.selectedType.set('compulsa'); fixture.detectChanges(); internals.generate();
    expect(search.execute).toHaveBeenLastCalledWith(expect.objectContaining({ type: 'compulsa' }));
  });

  it.each(['rectificaciones', 'compulsa', 'anexo30-revision-entradas', 'anexo30-revision-fracciones', 'anexo30-revision-descargas', 'anexo30-revision-comparativa'] as const)('regresion: %s sigue como reporte textual', (type) => {
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set(type); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-operation-period-filter')).toBeNull();
    expect(fixture.nativeElement.querySelector('input[name=filter]')).not.toBeNull();
  });

  it('regresion: rectificaciones resumen conserva su contrato de consulta', () => {
    search.execute.mockReturnValue(of({ items: [{ pedimento: '26', total: 3 }], total: 1, page: 1, pageSize: 20 }));
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('rectificaciones'); fixture.detectChanges(); internals.generate(); fixture.detectChanges();
    expect(search.execute).toHaveBeenCalledWith(expect.objectContaining({ type: 'rectificaciones' }));
    expect(fixture.nativeElement.textContent).toContain('Rectificaciones');
  });



  it.each([
    'anexo30-revision-entradas',
    'anexo30-revision-fracciones',
    'anexo30-revision-descargas',
    'anexo30-revision-comparativa',
  ] as const)('habilita XLSX para %s generado', (type) => {
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set(type);
    fixture.detectChanges();
    internals.generate();
    fixture.detectChanges();

    expect(botones().some((button) => button.textContent?.includes('XLSX'))).toBe(true);
  });

  it('oculta XLSX de Anexo 30 sin REPORTES_EXPORTAR', () => {
    const auth = TestBed.inject(AuthService) as unknown as { hasPermission: () => boolean };
    auth.hasPermission = () => false;
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.selectedType.set('anexo30-revision-comparativa');
    internals.generate();
    fixture.detectChanges();

    expect(botones().some((button) => button.textContent?.includes('XLSX'))).toBe(false);
  });


  it('filtra tipos por etiqueta sin generar un reporte', () => {
    const page = fixture.componentInstance as unknown as { reportSearch: string; filteredReports(): { type: string }[]; hasGenerated(): boolean };
    page.reportSearch = '  CoMpUlSa  ';
    expect(page.filteredReports().map((report) => report.type)).toEqual(['compulsa', 'compulsa-detalle']);
    expect(page.hasGenerated()).toBe(false);
  });
  it('restaura filtros guardados sin generar reporte y no muta ante preset incompatible', () => {
    const internals = fixture.componentInstance as unknown as PageInternals;
    internals.currentPage = 4;
    const criteria: SavedCriteria = {
      type: 'entradas', from: null, to: null, customsDocument: 'DOC-1', customsCode: 'A1', tariffFraction: '', partNumber: '',
      material: '', product: '', userId: null, module: '', result: '', correlationId: '', filter: '',
    };
    internals.applySavedCriteria(criteria);
    expect(internals.selectedType()).toBe('entradas');
    expect(internals.customsDocument).toBe('DOC-1');
    expect(internals.currentPage).toBe(1);
    expect(search.execute).not.toHaveBeenCalled();
    const snapshot = { type: internals.selectedType(), customsDocument: internals.customsDocument, currentPage: internals.currentPage };
    internals.applySavedCriteria({ ...criteria, type: 'future-report' });
    expect({ type: internals.selectedType(), customsDocument: internals.customsDocument, currentPage: internals.currentPage }).toEqual(snapshot);
  });

});
