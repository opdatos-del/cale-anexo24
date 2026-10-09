import { Component, EventEmitter, Output, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { of } from 'rxjs';
import { NotificationService } from '@core/notifications/notification.service';
import { SavedCriteria } from '@features/saved-queries/domain/saved-query.model';
import { OperationPeriod, OperationPeriodFilterComponent } from '@features/operations/shared/presentation/operation-period-filter/operation-period-filter.component';
import { UsedMaterial, UsedMaterialSearchCriteria } from '@features/operations/usedmaterials/domain/models/used-material.model';
import { SearchUsedMaterialsUseCase } from '@features/operations/usedmaterials/application/use-cases/search-used-materials.use-case';
import { UsedMaterialListPage } from './used-material-list.page';

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

interface UsedMaterialHarness {
  fromDate: Date | null;
  toDate: Date | null;
  material: string;
  product: string;
  exitCustomsDocument: string;
  exitCustomsCode: string;
  currentPage: number;
  pageSize: number;
  items: WritableSignal<UsedMaterial[]>;
  totalItems: WritableSignal<number>;
  isLoading: WritableSignal<boolean>;
  hasSearched: WritableSignal<boolean>;
  applySavedCriteria(criteria: SavedCriteria): void;
  canExport(): boolean;
  exportCsv(): Promise<void>;
}

function configure() {
  const row: UsedMaterial = {
    dischargeId: 1,
    entryId: 2,
    entryLineId: 3,
    exitId: 4,
    exitLineId: 5,
    entryCustomsDocument: 'ENTRY-1',
    exitCustomsDocument: 'EXIT-1',
    materialCode: 'MAT-1',
    materialDescription: 'Material 1',
    productCode: 'PROD-1',
    productDescription: 'Product 1',
    incorporatedQuantity: '001.2300',
    wasteQuantity: 0.125,
    scrapQuantity: null,
    totalDischargedQuantity: '001.3550',
    unit: 'KG',
    date: '2026-06-15T23:45:00-06:00',
  };
  const execute = vi.fn((criteria: UsedMaterialSearchCriteria) => of({ items: [row], total: 201, page: criteria.page, pageSize: criteria.pageSize }));
  const notifications = { success: vi.fn(), info: vi.fn(), error: vi.fn(), warning: vi.fn() };
  TestBed.configureTestingModule({
    imports: [UsedMaterialListPage],
    providers: [
      { provide: SearchUsedMaterialsUseCase, useValue: { execute } },
      { provide: NotificationService, useValue: notifications },
    ],
  });
  TestBed.overrideComponent(UsedMaterialListPage, {
    remove: { imports: [OperationPeriodFilterComponent] },
    add: { imports: [PeriodFilterStub] },
  });
  const fixture = TestBed.createComponent(UsedMaterialListPage);
  fixture.detectChanges();
  const page = fixture.componentInstance as unknown as UsedMaterialHarness;
  return { page, execute, notifications };
}

describe('UsedMaterialListPage CSV export', () => {
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

  it('exporta todos los materiales con filtros actuales, fecha y cantidades crudas', async () => {
    const { page, execute } = configure();
    page.fromDate = new Date(2026, 5, 1);
    page.toDate = new Date(2026, 5, 30);
    page.material = 'MAT-FILTER';
    page.product = 'PROD-FILTER';
    page.exitCustomsDocument = 'EXIT-FILTER';
    page.exitCustomsCode = 'A1';
    page.currentPage = 4;
    page.pageSize = 50;
    page.hasSearched.set(true);
    page.totalItems.set(201);
    const visibleRows = page.items();

    await page.exportCsv();

    expect(execute.mock.calls.map(([criteria]) => [criteria.page, criteria.pageSize])).toEqual([[1, 100], [2, 100], [3, 100]]);
    expect(execute).toHaveBeenCalledWith(expect.objectContaining({ from: '2026-06-01', to: '2026-06-30', material: 'MAT-FILTER', product: 'PROD-FILTER', exitCustomsDocument: 'EXIT-FILTER', exitCustomsCode: 'A1' }));
    expect(page.currentPage).toBe(4);
    expect(page.pageSize).toBe(50);
    expect(page.items()).toBe(visibleRows);
    expect(createObjectUrlSpy).toHaveBeenCalledOnce();
    expect(downloadedFilename).toBe('materiales-utilizados.csv');
    expect(exportedBlob).toBeInstanceOf(Blob);
    expect(exportedBlob?.type).toBe('text/csv;charset=utf-8');
    const csv = await readBlob(exportedBlob!);
    expect(csv).toContain('2026-06-15,ENTRY-1,EXIT-1,MAT-1,Material 1,PROD-1,Product 1,001.2300,0.125,,001.3550,KG');
    expect(csv).not.toContain('dischargeId');
    expect(csv).not.toContain('entryLineId');
    expect(csv).not.toContain('exitLineId');
  });

  it('aborta al rebasar 10,000 y conserva las filas mostradas', async () => {
    const { page, execute, notifications } = configure();
    page.fromDate = new Date(2026, 5, 1);
    page.toDate = new Date(2026, 5, 30);
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

  it('no permite exportar sin periodo válido ni durante consulta', () => {
    const { page, execute } = configure();
    page.hasSearched.set(true);
    page.totalItems.set(1);
    expect(page.canExport()).toBe(false);
    page.fromDate = new Date(2026, 5, 2);
    page.toDate = new Date(2026, 5, 1);
    expect(page.canExport()).toBe(false);
    page.toDate = new Date(2026, 5, 3);
    page.isLoading.set(true);
    expect(page.canExport()).toBe(false);
    expect(execute).not.toHaveBeenCalled();
  });
  it('aplica filtros completos y rechaza presets obsoletos atomicamente', () => {
    const { page, notifications } = configure();
    page.currentPage = 5;
    page.applySavedCriteria({ from: '2026-06-01', to: '2026-06-30', material: 'MAT-1', product: 'PROD-1', exitCustomsDocument: 'EXIT-1', exitCustomsCode: 'A1' });
    expect(page.currentPage).toBe(1);
    expect(page.material).toBe('MAT-1');
    expect(page.product).toBe('PROD-1');
    expect(page.exitCustomsDocument).toBe('EXIT-1');
    expect(page.exitCustomsCode).toBe('A1');
    const snapshot = { currentPage: page.currentPage, material: page.material, product: page.product, exitCustomsDocument: page.exitCustomsDocument, exitCustomsCode: page.exitCustomsCode };
    page.applySavedCriteria({ from: '2026-06-01', to: 'invalid-06-30', material: 'MAT-1', product: 'PROD-1', exitCustomsDocument: 'EXIT-1', exitCustomsCode: 'A1' });
    expect({ currentPage: page.currentPage, material: page.material, product: page.product, exitCustomsDocument: page.exitCustomsDocument, exitCustomsCode: page.exitCustomsCode }).toEqual(snapshot);
    expect(notifications.error).toHaveBeenCalledWith('Esta consulta guardada ya no es compatible con esta pantalla.');
  });

});
