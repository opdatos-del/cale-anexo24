import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { OperationSearchCriteria } from '@features/operations/shared/operation-search-criteria';
import { ExitPage, ExitRepository } from '@features/operations/exits/domain/repositories/exit.repository';
import { ExitApi } from '../api/exit.api';
import { ExitMapper } from '../mappers/exit.mapper';

/** Implementación HTTP del puerto de Salidas. */
@Injectable()
export class HttpExitRepository implements ExitRepository {
  private readonly api = inject(ExitApi);

  search(criteria: OperationSearchCriteria): Observable<ExitPage> {
    return this.api.search(criteria).pipe(map(ExitMapper.toPage));
  }
}
