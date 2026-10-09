import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Observable, of } from 'rxjs';
import { PedimentError } from '@features/operations/pediments/domain/models/pediment-upload.model';
import { GetPedimentErrorsUseCase } from './use-cases/get-pediment-errors.use-case';
import { PedimentImportErrorCsvService, pedimentImportErrorsCsv } from './pediment-import-error-csv.service';

function error(value = 'valor'): PedimentError {
  return { hoja: 'Hoja, 1', fila: null, columna: 'Texto', valorEnmascarado: value, codigo: 'COD', mensaje: 'Linea\ndos' };
}

function configure(execute: (...args: unknown[]) => Observable<PedimentError[]> = vi.fn(() => of<PedimentError[]>([]))) {
  TestBed.configureTestingModule({ providers: [PedimentImportErrorCsvService, { provide: GetPedimentErrorsUseCase, useValue: { execute } }] });
  return { service: TestBed.inject(PedimentImportErrorCsvService), execute };
}

describe('pedimentImportErrorsCsv', () => {
  it('includes BOM, quoted fields, nulls and formula protection', () => {
    const csv = pedimentImportErrorsCsv([error('=SUM(A1:A2)')]);
    expect(csv.startsWith('\uFEFFHoja,Fila,Columna,Valor,Código,Mensaje\r\n')).toBe(true);
    expect(csv).toContain('"Hoja, 1"');
    expect(csv).toContain(",'=SUM(A1:A2),");
    expect(csv).toContain('"Linea\ndos"');
  });
});

describe('PedimentImportErrorCsvService', () => {
  afterEach(() => vi.unstubAllGlobals());
  it('downloads all pages until a partial page', () => {
    const first = Array.from({ length: 100 }, () => error());
    const execute = vi.fn((_id, page) => of(page === 1 ? first : [error('fin')]));
    const { service } = configure(execute);
    vi.stubGlobal('URL', { createObjectURL: vi.fn(() => 'blob:csv'), revokeObjectURL: vi.fn() });
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    service.download(42).subscribe();
    expect(execute).toHaveBeenNthCalledWith(1, 42, 1, 100);
    expect(execute).toHaveBeenNthCalledWith(2, 42, 2, 100);
    expect(click).toHaveBeenCalledOnce();
  });

  it('rejects when errors exceed 10,000', () => {
    const full = Array.from({ length: 100 }, () => error());
    const execute = vi.fn((_id, page) => of(page <= 100 ? full : [error('extra')]));
    const { service } = configure(execute);
    const failure = vi.fn();
    service.download(42).subscribe({ error: failure });
    expect(execute).toHaveBeenCalledTimes(101);
    expect(failure).toHaveBeenCalledWith(expect.objectContaining({ message: 'La descarga supera el máximo de 10,000 errores.' }));
  });
});
