import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { OperationSearchCriteria } from '@features/operations/shared/operation-search-criteria';
import { EntryPage, EntryRepository } from '../../domain/repositories/entry.repository';

/** Caso de uso de consulta paginada de líneas de entrada. */
@Injectable({ providedIn: 'root' })
export class SearchEntriesUseCase {
  private readonly repository = inject(EntryRepository);

  execute(criteria: OperationSearchCriteria): Observable<EntryPage> {
    return this.repository.search(criteria);
  }
}
