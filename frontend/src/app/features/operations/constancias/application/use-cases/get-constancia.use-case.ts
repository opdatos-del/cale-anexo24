import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConstanciaLoad } from '@features/operations/constancias/domain/models/constancia-import.model';
import { ConstanciaImportRepository } from '@features/operations/constancias/domain/repositories/constancia-import.repository';

/** Recupera una carga de constancias persistida para refrescar su estado real. */
@Injectable({ providedIn: 'root' })
export class GetConstanciaUseCase {
  private readonly repository = inject(ConstanciaImportRepository);

  execute(id: number, page = 1, pageSize = 100): Observable<ConstanciaLoad> {
    return this.repository.get(id, page, pageSize);
  }
}
