import { Observable } from 'rxjs';
import { ReportPage, ReportSearchCriteria } from '@features/reports/domain/models/report.model';

/** Puerto de consulta y exportación de los reportes V1. */
export abstract class ReportRepository {
  abstract search(criteria: ReportSearchCriteria): Observable<ReportPage>;
  abstract export(criteria: ReportSearchCriteria): Observable<Blob>;
}
