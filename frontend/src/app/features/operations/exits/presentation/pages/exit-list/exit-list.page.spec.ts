import { Component, EventEmitter, Output, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { of } from 'rxjs';
import { NotificationService } from '@core/notifications/notification.service';
import { SavedCriteria } from '@features/saved-queries/domain/saved-query.model';
import { OperationPeriod, OperationPeriodFilterComponent } from '@features/operations/shared/presentation/operation-period-filter/operation-period-filter.component';
import { OperationSearchCriteria } from '@features/operations/shared/operation-search-criteria';
import { ExitLine } from '@features/operations/exits/domain/models/exit-line.model';
import { SearchExitsUseCase } from '@features/operations/exits/application/use-cases/search-exits.use-case';
import { ExitListPage } from './exit-list.page';

let exportedBlob: Blob | undefined;
let downloadedFilename: string | undefined;
let createObjectUrlSpy: ReturnType<typeof vi.spyOn>;
let revokeObjectUrlSpy: ReturnType<typeof vi.spyOn>;
let anchorClickSpy: ReturnType<typeof vi.spyOn>;

function readBlob(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error);
    reader.readAsText(blob);
  });
}


@Component({ selector: 'app-operation-period-filter', standalone: true, template: '' })
class PeriodFilterStub {
  @Output() readonly periodChange = new EventEmitter<OperationPeriod>();
  clear(): void { this.periodChange.emit({ start: null, end: null }); }
  period: OperationPeriod | null = null;
  set(period: OperationPeriod): void { this.period = period; }
}

interface ExitHarness {
  fromDate: Date | null;
  toDate: Date | null;
  customsDocument: string;
  customsCode: string;
  tariffFraction: string;
  partNumber: string;
  currentPage: number;
  pageSize: number;
  items: WritableSignal<ExitLine[]>;
  totalItems: WritableSignal<number>;
  isLoading: WritableSignal<boolean>;
  isExporting: WritableSignal<boolean>;
  hasSearched: WritableSignal<boolean>;
  applySavedCriteria(criteria: SavedCriteria): void;
  canExport(): boolean;
  exportCsv(): Promise<void>;
}

function configure() {
  const row: ExitLine = {
    exitId: 456,
    lineId: 789,
    customsDocument: 'DOC-1',
    customsCode: 'A1',
    paymentDate: '2026-05-08T00:30:00Z',
    tariffFraction: '9876.54',
    commercialUnit: 'PZA',
    quantity: -125.5,
    partNumber: 'PART-2',
  };
  const execute = vi.fn((criteria: OperationSearchCriteria) => of({ items: [row], total: 101, page: criteria.page, pageSize: criteria.pageSize }));
  const notifications = { success: vi.fn(), info: vi.fn(), error: vi.fn(), warning: vi.fn() };
  TestBed.configureTestingModule({
    imports: [ExitListPage],
    providers: [
      { provide: SearchExitsUseCase, useValue: { execute } },
      { provide: NotificationService, useValue: notifications },
    ],
  });
  TestBed.overrideComponent(ExitListPage, {
    remove: { imports: [OperationPeriodFilterComponent] },
    add: { imports: [PeriodFilterStub] },
  });
  const fixture = TestBed.createComponent(ExitListPage);
  fixture.detectChanges();
  const page = fixture.componentInstance as unknown as ExitHarness;
  return { page, execute, notifications };
}

describe('ExitListPage CSV export', () => {
  beforeEach(() => {
    exportedBlob = undefined;
    downloadedFilename = undefined;
    createObjectUrlSpy = vi.spyOn(URL, 'createObjectURL').mockImplementation((blob) => {
      exportedBlob = blob as Blob;
      return 'blob:csv';
    });
    revokeObjectUrlSpy = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    anchorClickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      downloadedFilename = this.download;
    });
  });

  afterEach(() => {
    createObjectUrlSpy.mockRestore();
    revokeObjectUrlSpy.mockRestore();
    anchorClickSpy.mockRestore();
  });

  it('exporta páginas completas con criterios actuales y conserva página visible', async () => {
    const { page, execute } = configure();
    page.fromDate = new Date(2026, 4, 1);
    page.toDate = new Date(2026, 4, 31);
    page.customsDocument = 'DOC-FILTER';
    page.customsCode = 'A1';
    page.tariffFraction = '9876';
    page.partNumber = 'P-2';
    page.currentPage = 2;
    page.pageSize = 50;
    page.hasSearched.set(true);
    page.totalItems.set(101);
    const visibleRows = page.items();

    await page.exportCsv();

    expect(execute.mock.calls.map(([criteria]) => [criteria.page, criteria.pageSize])).toEqual([[1, 100], [2, 100]]);
    expect(execute).toHaveBeenCalledWith(expect.objectContaining({ from: '2026-05-01', to: '2026-05-31', customsDocument: 'DOC-FILTER', customsCode: 'A1', tariffFraction: '9876', partNumber: 'P-2' }));
    expect(page.currentPage).toBe(2);
    expect(page.pageSize).toBe(50);
    expect(page.items()).toBe(visibleRows);
    expect(createObjectUrlSpy).toHaveBeenCalledOnce();
    expect(downloadedFilename).toBe('salidas.csv');
    expect(exportedBlob).toBeInstanceOf(Blob);
    expect(exportedBlob?.type).toBe('text/csv;charset=utf-8');
    const csv = await readBlob(exportedBlob!);
    expect(csv).toContain('2026-05-08,9876.54,PZA,-125.5,PART-2');
    expect(csv).not.toContain('exitId');
    expect(csv).not.toContain('lineId');
  });

  it('limita exportación a 10,000 filas y preserva tabla en error', async () => {
    const { page, execute, notifications } = configure();
    page.fromDate = new Date(2026, 4, 1);
    page.toDate = new Date(2026, 4, 31);
    page.hasSearched.set(true);
    page.totalItems.set(10_001);
    const visibleRows = page.items();
    execute.mockImplementation((criteria) => of({ items: [], total: 10_001, page: criteria.page, pageSize: criteria.pageSize }));

    await page.exportCsv();

    expect(execute).toHaveBeenCalledOnce();
    expect(notifications.error).toHaveBeenCalledWith('La exportación supera el máximo de 10,000 registros.');
    expect(page.items()).toBe(visibleRows);
    expect(createObjectUrlSpy).not.toHaveBeenCalled();
  });

  it('bloquea exportación cuando periodo inválido o ya hay carga', () => {
    const { page, execute } = configure();
    page.hasSearched.set(true);
    page.totalItems.set(1);
    expect(page.canExport()).toBe(false);
    page.fromDate = new Date(2026, 4, 2);
    page.toDate = new Date(2026, 4, 1);
    expect(page.canExport()).toBe(false);
    page.toDate = new Date(2026, 4, 3);
    page.isLoading.set(true);
    expect(page.canExport()).toBe(false);
    expect(execute).not.toHaveBeenCalled();
  });
  it('aplica filtros completos y rechaza presets obsoletos atomicamente', () => {
    const { page, notifications } = configure();
    page.currentPage = 5;
    page.applySavedCriteria({ from: '2026-05-01', to: '2026-05-31', customsDocument: 'DOC-SAVED', customsCode: 'A1', tariffFraction: '5678', partNumber: 'PART-2' });
    expect(page.currentPage).toBe(1);
    expect(page.customsDocument).toBe('DOC-SAVED');
    expect(page.customsCode).toBe('A1');
    expect(page.tariffFraction).toBe('5678');
    expect(page.partNumber).toBe('PART-2');
    const snapshot = { currentPage: page.currentPage, customsDocument: page.customsDocument, customsCode: page.customsCode, tariffFraction: page.tariffFraction, partNumber: page.partNumber };
    page.applySavedCriteria({ from: '2026-05-01', to: 'invalid-05-31', customsDocument: 'DOC-SAVED', customsCode: 'A1', tariffFraction: '5678', partNumber: 'PART-2' });
    expect({ currentPage: page.currentPage, customsDocument: page.customsDocument, customsCode: page.customsCode, tariffFraction: page.tariffFraction, partNumber: page.partNumber }).toEqual(snapshot);
    expect(notifications.error).toHaveBeenCalledWith('Esta consulta guardada ya no es compatible con esta pantalla.');
  });

});
