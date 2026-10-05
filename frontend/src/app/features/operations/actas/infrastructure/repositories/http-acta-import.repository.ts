import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActaConfirmation, ActaError, ActaLoad } from '@features/operations/actas/domain/models/acta-import.model';
import { ActaImportRepository } from '@features/operations/actas/domain/repositories/acta-import.repository';
import { ActaImportApiService } from '@features/operations/actas/infrastructure/api/acta-import-api.service';

/** Implementación HTTP del puerto de carga de actas. */
@Injectable({ providedIn: 'root' })
export class HttpActaImportRepository implements ActaImportRepository {
  private readonly api = inject(ActaImportApiService);

  upload(file: File): Observable<ActaLoad> {
    return this.api.upload(file);
  }

  get(id: number, page: number, pageSize: number): Observable<ActaLoad> {
    return this.api.get(id, page, pageSize);
  }

  errors(id: number, page: number, pageSize: number): Observable<ActaError[]> {
    return this.api.errors(id, page, pageSize);
  }

  confirm(id: number): Observable<ActaConfirmation> {
    return this.api.confirm(id);
  }
}
