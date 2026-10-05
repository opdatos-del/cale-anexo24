import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActaLoad } from '@features/operations/actas/domain/models/acta-import.model';
import { ActaImportRepository } from '@features/operations/actas/domain/repositories/acta-import.repository';

/** Recupera una carga de actas persistida para refrescar su estado real. */
@Injectable({ providedIn: 'root' })
export class GetActaUseCase {
  private readonly repository = inject(ActaImportRepository);

  execute(id: number, page = 1, pageSize = 100): Observable<ActaLoad> {
    return this.repository.get(id, page, pageSize);
  }
}
