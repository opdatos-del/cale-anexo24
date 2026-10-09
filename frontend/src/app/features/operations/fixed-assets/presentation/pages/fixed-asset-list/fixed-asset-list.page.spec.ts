import { Component, EventEmitter, Output, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { of } from 'rxjs';
import { NotificationService } from '@core/notifications/notification.service';
import { OperationPeriod, OperationPeriodFilterComponent } from '@features/operations/shared/presentation/operation-period-filter/operation-period-filter.component';
import { FixedAsset, FixedAssetSearchCriteria } from '@features/operations/fixed-assets/domain/models/fixed-asset.model';
import { SearchFixedAssetsUseCase } from '@features/operations/fixed-assets/application/use-cases/search-fixed-assets.use-case';
import { FixedAssetListPage } from './fixed-asset-list.page';

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
}

interface FixedAssetHarness {
  fromDate: Date | null;
  toDate: Date | null;
  customsDocument: string;
  customsCode: string;
  partNumber: string;
  description: string;
  serialNumber: string;
  brand: string;
  model: string;
  currentPage: number;
  pageSize: number;
  items: WritableSignal<FixedAsset[]>;
  totalItems: WritableSignal<number>;
  isLoading: WritableSignal<boolean>;
  hasSearched: WritableSignal<boolean>;
  canExport(): boolean;
  exportCsv(): Promise<void>;
}

async function configure() {
  const row: FixedAsset = {
    entryLineId: 101,
    importId: 202,
    customsDocument: 'DOC-1',
    customsCode: 'A1',
    importDate: '2026-07-19T23:30:00-06:00',
    partNumber: 'PART-1',
    description: 'Equipo de prueba',
    tariffFraction: '1234.56',
    quantity: '0002.5000',
    unit: 'PZA',
    serialNumber: 'SER-1',
    brand: 'Marca',
    model: 'Modelo',
  };
  const execute = vi.fn((criteria: FixedAssetSearchCriteria) => of({ items: [row], total: 101, page: criteria.page, pageSize: criteria.pageSize }));
  const notifications = { success: vi.fn(), info: vi.fn(), error: vi.fn(), warning: vi.fn() };
  TestBed.configureTestingModule({
    imports: [FixedAssetListPage],
    providers: [
      { provide: SearchFixedAssetsUseCase, useValue: { execute } },
      { provide: NotificationService, useValue: notifications },
    ],
  });
  TestBed.overrideComponent(FixedAssetListPage, {
    remove: { imports: [OperationPeriodFilterComponent] },
    add: { imports: [PeriodFilterStub] },
  });
  const fixture = TestBed.createComponent(FixedAssetListPage);
  fixture.detectChanges();
  await fixture.whenStable();
  const page = fixture.componentInstance as unknown as FixedAssetHarness;
  return { page, execute, notifications };
}

describe('FixedAssetListPage CSV export', () => {
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

  it('permite exportar sin periodo y usa filtros actuales en todas las páginas', async () => {
    const { page, execute } = await configure();
    page.customsDocument = 'DOC-FILTER';
    page.customsCode = 'A1';
    page.partNumber = 'P-FILTER';
    page.description = 'Equipo';
    page.serialNumber = 'SER-FILTER';
    page.brand = 'Marca-FILTER';
    page.model = 'Modelo-FILTER';
    page.isLoading.set(false);
    page.hasSearched.set(true);
    page.totalItems.set(101);
    page.currentPage = 3;
    page.pageSize = 50;
    const visibleRows = page.items();
    expect(page.fromDate).toBeNull();
    expect(page.toDate).toBeNull();
    expect(page.canExport()).toBe(true);
    execute.mockClear();

    await page.exportCsv();

    expect(execute.mock.calls.map(([criteria]) => [criteria.page, criteria.pageSize])).toEqual([[1, 100], [2, 100]]);
    expect(execute).toHaveBeenCalledWith(expect.objectContaining({ from: null, to: null, customsDocument: 'DOC-FILTER', customsCode: 'A1', partNumber: 'P-FILTER', description: 'Equipo', serialNumber: 'SER-FILTER', brand: 'Marca-FILTER', model: 'Modelo-FILTER' }));
    expect(page.currentPage).toBe(3);
    expect(page.pageSize).toBe(50);
    expect(page.items()).toBe(visibleRows);
    expect(createObjectUrlSpy).toHaveBeenCalledOnce();
    expect(downloadedFilename).toBe('activos-fijos.csv');
    expect(exportedBlob).toBeInstanceOf(Blob);
    expect(exportedBlob?.type).toBe('text/csv;charset=utf-8');
    const csv = await readBlob(exportedBlob!);
    expect(csv).toContain('2026-07-19,DOC-1,A1,PART-1,Equipo de prueba,1234.56,0002.5000,PZA,SER-1,Marca,Modelo');
    expect(csv).not.toContain('entryLineId');
    expect(csv).not.toContain('importId');
  });

  it('permite periodo completo, env�a ambas fechas y bloquea rango parcial o invertido', async () => {
    const { page, execute } = await configure();
    page.isLoading.set(false);
    page.hasSearched.set(true);
    page.totalItems.set(101);
    page.fromDate = new Date(2026, 6, 1);
    expect(page.canExport()).toBe(false);
    page.toDate = new Date(2026, 6, 2);
    expect(page.canExport()).toBe(true);
    execute.mockClear();
    await page.exportCsv();
    expect(execute).toHaveBeenCalledWith(expect.objectContaining({ from: '2026-07-01', to: '2026-07-02' }));
    page.fromDate = new Date(2026, 6, 3);
    expect(page.canExport()).toBe(false);
  });

  it('aborta límite sobre 10,000 con una llamada y mantiene tabla', async () => {
    const { page, execute, notifications } = await configure();
    page.isLoading.set(false);
    page.hasSearched.set(true);
    page.totalItems.set(10_001);
    const visibleRows = page.items();
    execute.mockClear();
    execute.mockImplementation((criteria) => of({ items: [], total: 10_001, page: criteria.page, pageSize: criteria.pageSize }));

    await page.exportCsv();

    expect(execute).toHaveBeenCalledOnce();
    expect(notifications.error).toHaveBeenCalledWith('La exportación supera el máximo de 10,000 registros.');
    expect(page.items()).toBe(visibleRows);
    expect(createObjectUrlSpy).not.toHaveBeenCalled();
  });
});
