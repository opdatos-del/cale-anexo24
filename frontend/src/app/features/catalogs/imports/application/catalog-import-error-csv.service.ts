import { Injectable, inject } from '@angular/core';
import { Observable, concatMap, defer, expand, map, reduce, throwError } from 'rxjs';
import { CatalogImportError, CatalogImportType } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { GetCatalogImportErrorsUseCase } from './use-cases/get-catalog-import-errors.use-case';

const PAGE_SIZE = 100;
export const MAXIMUM_CATALOG_IMPORT_ERRORS_EXPORT = 10_000;

/** Descarga segura de errores de validación persistidos y paginados por backend. */
@Injectable({ providedIn: 'root' })
export class CatalogImportErrorCsvService {
  private readonly getErrors = inject(GetCatalogImportErrorsUseCase);

  download(type: CatalogImportType, cargaId: number): Observable<void> {
    return this.loadAll(type, cargaId).pipe(
      map((errors) => new Blob([catalogImportErrorsCsv(errors)], { type: 'text/csv;charset=utf-8' })),
      map((file) => this.downloadFile(file, 'errores-' + fileType(type) + '-' + cargaId + '.csv')),
    );
  }

  private loadAll(type: CatalogImportType, cargaId: number): Observable<CatalogImportError[]> {
    return defer(() => this.getErrors.execute(type, cargaId, 1, PAGE_SIZE)).pipe(
      expand((page, index) => {
        const loaded = (index + 1) * PAGE_SIZE;
        if (page.length < PAGE_SIZE) return [];
        if (loaded >= MAXIMUM_CATALOG_IMPORT_ERRORS_EXPORT) {
          return this.getErrors.execute(type, cargaId, index + 2, PAGE_SIZE).pipe(
            concatMap((next) => next.length ? throwError(() => new Error('La descarga supera el máximo de 10,000 errores.')) : []),
          );
        }
        return this.getErrors.execute(type, cargaId, index + 2, PAGE_SIZE);
      }),
      reduce((all, page) => {
        const next = all.concat(page);
        if (next.length > MAXIMUM_CATALOG_IMPORT_ERRORS_EXPORT) {
          throw new Error('La descarga supera el máximo de 10,000 errores.');
        }
        return next;
      }, [] as CatalogImportError[]),
    );
  }

  private downloadFile(file: Blob, filename: string): void {
    const url = URL.createObjectURL(file);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}

export function catalogImportErrorsCsv(errors: CatalogImportError[]): string {
  const header = ['Hoja', 'Fila', 'Columna', 'Valor', 'C\u00f3digo', 'Mensaje'];
  const rows = errors.map((error) => [
    error.hoja,
    error.fila,
    error.columna,
    error.valorEnmascarado,
    error.codigo,
    error.mensaje,
  ].map(csvField).join(','));
  return '\uFEFF' + header.join(',') + '\r\n' + rows.join('\r\n');
}

function csvField(value: string | number | null): string {
  const text = value === null ? '' : String(value);
  const safe = /^[=+\-@]/.test(text) ? "'" + text : text;
  return /[",\r\n]/.test(safe) ? '"' + safe.replaceAll('"', '""') + '"' : safe;
}

function fileType(type: CatalogImportType): string {
  switch (type) {
    case 'MATERIAL': return 'material';
    case 'PRODUCTO': return 'producto';
    case 'CLIENTE': return 'cliente';
    case 'PROVEEDOR': return 'proveedor';
    case 'AGENTE': return 'agente';
  }
}
