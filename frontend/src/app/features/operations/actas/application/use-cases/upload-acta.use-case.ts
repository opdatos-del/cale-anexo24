import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActaLoad } from '@features/operations/actas/domain/models/acta-import.model';
import { ActaImportRepository } from '@features/operations/actas/domain/repositories/acta-import.repository';

/** Valida y guarda en staging una carga de actas previsualizada. */
@Injectable({ providedIn: 'root' })
export class UploadActaUseCase {
  private readonly repository = inject(ActaImportRepository);

  execute(file: File): Observable<ActaLoad> {
    return this.repository.upload(file);
  }
}
