import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActaConfirmation } from '@features/operations/actas/domain/models/acta-import.model';
import { ActaImportRepository } from '@features/operations/actas/domain/repositories/acta-import.repository';

/** Confirma de forma autoritativa una carga de actas previsualizada. */
@Injectable({ providedIn: 'root' })
export class ConfirmActaUseCase {
  private readonly repository = inject(ActaImportRepository);

  execute(id: number): Observable<ActaConfirmation> {
    return this.repository.confirm(id);
  }
}
