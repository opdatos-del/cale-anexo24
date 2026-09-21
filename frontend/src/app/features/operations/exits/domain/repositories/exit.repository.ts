import { Observable } from 'rxjs';
import { OperationSearchCriteria } from '@features/operations/shared/operation-search-criteria';
import { ExitLine } from '../models/exit-line.model';

export interface ExitPage {
  items: ExitLine[];
  total: number;
  page: number;
  pageSize: number;
}

/** Puerto de consulta de líneas de salida. */
export abstract class ExitRepository {
  abstract search(criteria: OperationSearchCriteria): Observable<ExitPage>;
}
