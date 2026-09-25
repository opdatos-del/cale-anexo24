import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { OperationSearchCriteria } from '@features/operations/shared/operation-search-criteria';
import { ExitPage, ExitRepository } from '@features/operations/exits/domain/repositories/exit.repository';

/** Caso de uso de consulta paginada de líneas de salida. */
@Injectable({ providedIn: 'root' })
export class SearchExitsUseCase {
  private readonly repository = inject(ExitRepository);

  execute(criteria: OperationSearchCriteria): Observable<ExitPage> {
    return this.repository.search(criteria);
  }
}
