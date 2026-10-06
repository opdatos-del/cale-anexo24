import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConstanciaConfirmation, ConstanciaError, ConstanciaLoad } from '@features/operations/constancias/domain/models/constancia-import.model';
import { ConstanciaImportRepository } from '@features/operations/constancias/domain/repositories/constancia-import.repository';
import { ConstanciaImportApiService } from '@features/operations/constancias/infrastructure/api/constancia-import-api.service';

/** Implementación HTTP del puerto de carga de constancias. */
@Injectable({ providedIn: 'root' })
export class HttpConstanciaImportRepository implements ConstanciaImportRepository {
  private readonly api = inject(ConstanciaImportApiService);

  upload(file: File): Observable<ConstanciaLoad> {
    return this.api.upload(file);
  }

  get(id: number, page: number, pageSize: number): Observable<ConstanciaLoad> {
    return this.api.get(id, page, pageSize);
  }

  errors(id: number, page: number, pageSize: number): Observable<ConstanciaError[]> {
    return this.api.errors(id, page, pageSize);
  }

  confirm(id: number): Observable<ConstanciaConfirmation> {
    return this.api.confirm(id);
  }
}
