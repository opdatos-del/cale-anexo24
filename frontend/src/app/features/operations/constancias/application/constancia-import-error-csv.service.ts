import { Injectable, inject } from '@angular/core';
import { Observable, concatMap, defer, expand, map, reduce, throwError } from 'rxjs';
import { ConstanciaError } from '@features/operations/constancias/domain/models/constancia-import.model';
import { GetConstanciaErrorsUseCase } from './use-cases/get-constancia-errors.use-case';

const PAGE_SIZE = 100;
export const MAXIMUM_CONSTANCIA_IMPORT_ERRORS_EXPORT = 10_000;

/** Descarga segura de errores de validación persistidos y paginados de constancias. */
@Injectable({ providedIn: 'root' })
export class ConstanciaImportErrorCsvService {
  private readonly getErrors = inject(GetConstanciaErrorsUseCase);

  download(cargaId: number): Observable<void> {
    return this.loadAll(cargaId).pipe(
      map((errors) => new Blob([constanciaImportErrorsCsv(errors)], { type: 'text/csv;charset=utf-8' })),
      map((file) => this.downloadFile(file, `errores-constancia-${cargaId}.csv`)),
    );
  }

  private loadAll(cargaId: number): Observable<ConstanciaError[]> {
    return defer(() => this.getErrors.execute(cargaId, 1, PAGE_SIZE)).pipe(
      expand((page, index) => {
        const loaded = (index + 1) * PAGE_SIZE;
        if (page.length < PAGE_SIZE) return [];
        if (loaded >= MAXIMUM_CONSTANCIA_IMPORT_ERRORS_EXPORT) {
          return this.getErrors.execute(cargaId, index + 2, PAGE_SIZE).pipe(
            concatMap((next) => next.length ? throwError(() => new Error('La descarga supera el máximo de 10,000 errores.')) : []),
          );
        }
        return this.getErrors.execute(cargaId, index + 2, PAGE_SIZE);
      }),
      reduce((all, page) => {
        const next = all.concat(page);
        if (next.length > MAXIMUM_CONSTANCIA_IMPORT_ERRORS_EXPORT) throw new Error('La descarga supera el máximo de 10,000 errores.');
        return next;
      }, [] as ConstanciaError[]),
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

export function constanciaImportErrorsCsv(errors: ConstanciaError[]): string {
  const header = ['Hoja', 'Fila', 'Columna', 'Valor', 'Código', 'Mensaje'];
  const rows = errors.map((error) => [error.hoja, error.fila, error.columna, error.valorEnmascarado, error.codigo, error.mensaje].map(csvField).join(','));
  return '\uFEFF' + header.join(',') + '\r\n' + rows.join('\r\n');
}

function csvField(value: string | number | null): string {
  const text = value === null ? '' : String(value);
  const safe = /^[=+\-@]/.test(text) ? "'" + text : text;
  return /[",\r\n]/.test(safe) ? '"' + safe.replaceAll('"', '""') + '"' : safe;
}
