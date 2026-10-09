import { Component, EventEmitter, Output, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { of } from 'rxjs';
import { NotificationService } from '@core/notifications/notification.service';
import { SavedCriteria } from '@features/saved-queries/domain/saved-query.model';
import { OperationPeriod, OperationPeriodFilterComponent } from '@features/operations/shared/presentation/operation-period-filter/operation-period-filter.component';
import { OperationSearchCriteria } from '@features/operations/shared/operation-search-criteria';
import { EntryLine } from '@features/operations/entries/domain/models/entry-line.model';
import { SearchEntriesUseCase } from '@features/operations/entries/application/use-cases/search-entries.use-case';
import { EntryListPage } from './entry-list.page';

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

interface EntryHarness {
  fromDate: Date | null;
  toDate: Date | null;
  customsDocument: string;
  customsCode: string;
  tariffFraction: string;
  partNumber: string;
  currentPage: number;
  pageSize: number;
  items: WritableSignal<EntryLine[]>;
  totalItems: WritableSignal<number>;
  isLoading: WritableSignal<boolean>;
  isExporting: WritableSignal<boolean>;
  hasSearched: WritableSignal<boolean>;
  applySavedCriteria(criteria: SavedCriteria): void;
  canExport(): boolean;
  exportCsv(): Promise<void>;
}

function configure() {
  const row: EntryLine = {
    importId: 456,
    lineId: 789,
    customsDocument: 'DOC-1',
    customsCode: 'A1',
    entryDate: '2026-04-02T23:00:00-06:00',
    paymentDate: null,
    tariffFraction: '1234.56',
    commercialUnit: 'KG',
    quantity: '001.2300',
    partNumber: 'PART-1',
  };
  const execute = vi.fn((criteria: OperationSearchCriteria) => of({ items: [row], total: 201, page: criteria.page, pageSize: criteria.pageSize }));
  const notifications = { success: vi.fn(), info: vi.fn(), error: vi.fn(), warning: vi.fn() };
  TestBed.configureTestingModule({
    imports: [EntryListPage],
    providers: [
      { provide: SearchEntriesUseCase, useValue: { execute } },
      { provide: NotificationService, useValue: notifications },
    ],
  });
  TestBed.overrideComponent(EntryListPage, {
    remove: { imports: [OperationPeriodFilterComponent] },
    add: { imports: [PeriodFilterStub] },
  });
  const fixture = TestBed.createComponent(EntryListPage);
  fixture.detectChanges();
  const page = fixture.componentInstance as unknown as EntryHarness;
  return { fixture, page, execute, notifications, row };
}

describe('EntryListPage CSV export', () => {
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

  it('exports every page using current filters without mutating visible state or technical IDs', async () => {
    const { page, execute, notifications } = configure();
    page.fromDate = new Date(2026, 3, 1);
    page.toDate = new Date(2026, 3, 30);
    page.customsDocument = 'DOC-FILTER';
    page.customsCode = 'A1';
    page.tariffFraction = '1234';
    page.partNumber = 'P-1';
    page.currentPage = 3;
    page.pageSize = 50;
    page.hasSearched.set(true);
    page.totalItems.set(201);
    const visibleRows: EntryLine[] = [{ importId: 1, lineId: 2, customsDocument: 'VISIBLE', customsCode: null, entryDate: null, paymentDate: null, tariffFraction: null, commercialUnit: null, quantity: null, partNumber: null }];
    page.items.set(visibleRows);

    await page.exportCsv();

    expect(execute.mock.calls.map(([criteria]) => [criteria.page, criteria.pageSize])).toEqual([[1, 100], [2, 100], [3, 100]]);
    expect(execute).toHaveBeenCalledWith(expect.objectContaining({ from: '2026-04-01', to: '2026-04-30', customsDocument: 'DOC-FILTER', customsCode: 'A1', tariffFraction: '1234', partNumber: 'P-1' }));
    expect(page.currentPage).toBe(3);
    expect(page.pageSize).toBe(50);
    expect(page.items()).toBe(visibleRows);
    expect(notifications.error).not.toHaveBeenCalled();
    expect(createObjectUrlSpy).toHaveBeenCalledOnce();
    expect(downloadedFilename).toBe('entradas.csv');
    expect(exportedBlob).toBeInstanceOf(Blob);
    expect(exportedBlob?.type).toBe('text/csv;charset=utf-8');
    const csv = await readBlob(exportedBlob!);
    expect(csv).toContain('2026-04-02,,1234.56,KG,001.2300,PART-1');
    expect(csv).not.toContain('importId');
    expect(csv).not.toContain('lineId');
    expect(page.isExporting()).toBe(false);
  });

  it('rechaza más de 10,000 filas con una sola consulta y conserva resultados', async () => {
    const { page, execute, notifications } = configure();
    page.fromDate = new Date(2026, 3, 1);
    page.toDate = new Date(2026, 3, 30);
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

  it('bloquea exportación sin periodo válido o mientras consulta carga', () => {
    const { page, execute } = configure();
    page.hasSearched.set(true);
    page.totalItems.set(1);
    expect(page.canExport()).toBe(false);
    page.fromDate = new Date(2026, 3, 2);
    page.toDate = new Date(2026, 3, 1);
    expect(page.canExport()).toBe(false);
    page.toDate = new Date(2026, 3, 3);
    page.isLoading.set(true);
    expect(page.canExport()).toBe(false);
    expect(execute).not.toHaveBeenCalled();
  });
  it('aplica filtros completos y rechaza presets obsoletos atomicamente', () => {
    const { page, notifications } = configure();
    page.currentPage = 5;
    page.applySavedCriteria({ from: '2026-04-01', to: '2026-04-30', customsDocument: 'DOC-SAVED', customsCode: 'A1', tariffFraction: '1234', partNumber: 'PART-1' });
    expect(page.currentPage).toBe(1);
    expect(page.customsDocument).toBe('DOC-SAVED');
    expect(page.customsCode).toBe('A1');
    expect(page.tariffFraction).toBe('1234');
    expect(page.partNumber).toBe('PART-1');
    const snapshot = { currentPage: page.currentPage, customsDocument: page.customsDocument, customsCode: page.customsCode, tariffFraction: page.tariffFraction, partNumber: page.partNumber };
    page.applySavedCriteria({ from: '2026-04-01', to: 'invalid-04-30', customsDocument: 'DOC-SAVED', customsCode: 'A1', tariffFraction: '1234', partNumber: 'PART-1' });
    expect({ currentPage: page.currentPage, customsDocument: page.customsDocument, customsCode: page.customsCode, tariffFraction: page.tariffFraction, partNumber: page.partNumber }).toEqual(snapshot);
    expect(notifications.error).toHaveBeenCalledWith('Esta consulta guardada ya no es compatible con esta pantalla.');
  });

});
