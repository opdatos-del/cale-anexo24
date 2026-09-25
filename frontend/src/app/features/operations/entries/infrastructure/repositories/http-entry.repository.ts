import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { OperationSearchCriteria } from '@features/operations/shared/operation-search-criteria';
import { EntryPage, EntryRepository } from '@features/operations/entries/domain/repositories/entry.repository';
import { EntryApi } from '@features/operations/entries/infrastructure/api/entry.api';
import { EntryMapper } from '@features/operations/entries/infrastructure/mappers/entry.mapper';

/** Implementación HTTP del puerto de Entradas. */
@Injectable()
export class HttpEntryRepository implements EntryRepository {
  private readonly api = inject(EntryApi);

  search(criteria: OperationSearchCriteria): Observable<EntryPage> {
    return this.api.search(criteria).pipe(map(EntryMapper.toPage));
  }
}
