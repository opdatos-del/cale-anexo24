import { Observable } from 'rxjs';
import { OperationSearchCriteria } from '@features/operations/shared/operation-search-criteria';
import { EntryLine } from '@features/operations/entries/domain/models/entry-line.model';

export interface EntryPage {
  items: EntryLine[];
  total: number;
  page: number;
  pageSize: number;
}

/** Puerto de consulta de líneas de entrada. */
export abstract class EntryRepository {
  abstract search(criteria: OperationSearchCriteria): Observable<EntryPage>;
}
