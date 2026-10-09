import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Observable, of } from 'rxjs';
import { ConstanciaError } from '@features/operations/constancias/domain/models/constancia-import.model';
import { GetConstanciaErrorsUseCase } from './use-cases/get-constancia-errors.use-case';
import { ConstanciaImportErrorCsvService, constanciaImportErrorsCsv } from './constancia-import-error-csv.service';

function error(value = 'valor'): ConstanciaError {
  return { hoja: 'Hoja, 1', fila: null, columna: 'Texto', valorEnmascarado: value, codigo: 'COD', mensaje: 'Linea\ndos' };
}

function configure(execute: (...args: unknown[]) => Observable<ConstanciaError[]> = vi.fn(() => of<ConstanciaError[]>([]))) {
  TestBed.configureTestingModule({ providers: [ConstanciaImportErrorCsvService, { provide: GetConstanciaErrorsUseCase, useValue: { execute } }] });
  return { service: TestBed.inject(ConstanciaImportErrorCsvService), execute };
}

describe('constanciaImportErrorsCsv', () => {
  it('includes BOM, quoted fields, nulls and formula protection', () => {
    const csv = constanciaImportErrorsCsv([error('@formula')]);
    expect(csv.startsWith('\uFEFFHoja,Fila,Columna,Valor,Código,Mensaje\r\n')).toBe(true);
    expect(csv).toContain('"Hoja, 1"');
    expect(csv).toContain(",'@formula,");
    expect(csv).toContain('"Linea\ndos"');
  });
});

describe('ConstanciaImportErrorCsvService', () => {
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
