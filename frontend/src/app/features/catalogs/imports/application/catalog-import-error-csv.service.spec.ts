import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Observable, of } from 'rxjs';
import { CatalogImportError } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { GetCatalogImportErrorsUseCase } from './use-cases/get-catalog-import-errors.use-case';
import { CatalogImportErrorCsvService, catalogImportErrorsCsv } from './catalog-import-error-csv.service';

function error(value = 'valor'): CatalogImportError {
  return { hoja: 'Datos', fila: 2, columna: 'Clave', valorEnmascarado: value, codigo: 'INVALIDO', mensaje: 'Mensaje' };
}

function configure(execute: (...args: unknown[]) => Observable<CatalogImportError[]> = vi.fn(() => of<CatalogImportError[]>([]))) {
  TestBed.configureTestingModule({
    providers: [
      CatalogImportErrorCsvService,
      { provide: GetCatalogImportErrorsUseCase, useValue: { execute } },
    ],
  });
  return { service: TestBed.inject(CatalogImportErrorCsvService), execute };
}

describe('catalogImportErrorsCsv', () => {
  it('includes BOM, headers, nulls, quoted values and formula protection', () => {
    const quote = String.fromCharCode(34);
    const csv = catalogImportErrorsCsv([{ hoja: 'Hoja, 1', fila: null, columna: 'Texto', valorEnmascarado: '=SUM(A1:A2)', codigo: 'COD', mensaje: 'Linea' + String.fromCharCode(10) + 'dos' }]);

    expect(csv.startsWith('\uFEFFHoja,Fila,Columna,Valor,C\u00f3digo,Mensaje\r\n')).toBe(true);
    expect(csv).toContain(quote + 'Hoja, 1' + quote);
    expect(csv).toContain(',\'=SUM(A1:A2),');
    expect(csv).toContain(quote + 'Linea' + String.fromCharCode(10) + 'dos' + quote);
  });
});

describe('CatalogImportErrorCsvService', () => {
  afterEach(() => vi.unstubAllGlobals());
  it('downloads all pages until a partial page', () => {
    const first = Array.from({ length: 100 }, () => error());
    const execute = vi.fn((_type, _id, page) => of(page === 1 ? first : [error('fin')]));
    const { service } = configure(execute);
    const createObjectURL = vi.fn(() => 'blob:csv');
    const revokeObjectURL = vi.fn();
    vi.stubGlobal('URL', { createObjectURL, revokeObjectURL });
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    service.download('CLIENTE', 81).subscribe();

    expect(execute).toHaveBeenNthCalledWith(1, 'CLIENTE', 81, 1, 100);
    expect(execute).toHaveBeenNthCalledWith(2, 'CLIENTE', 81, 2, 100);
    expect(click).toHaveBeenCalledOnce();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:csv');
  });

  it('rejects a download that exceeds 10,000 errors', () => {
    const complete = Array.from({ length: 100 }, () => error());
    const execute = vi.fn((_type, _id, page) => of(page <= 100 ? complete : [error('extra')]));
    const { service } = configure(execute);
    const failure = vi.fn();

    service.download('MATERIAL', 42).subscribe({ error: failure });

    expect(execute).toHaveBeenCalledTimes(101);
    expect(failure).toHaveBeenCalledWith(expect.objectContaining({ message: 'La descarga supera el m\u00e1ximo de 10,000 errores.' }));
  });
});
