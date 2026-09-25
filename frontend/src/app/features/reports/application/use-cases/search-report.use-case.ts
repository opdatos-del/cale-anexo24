import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ReportPage, ReportSearchCriteria } from '@features/reports/domain/models/report.model';
import { ReportRepository } from '@features/reports/domain/repositories/report.repository';

/** Caso de uso para generar un reporte paginado. */
@Injectable({ providedIn: 'root' })
export class SearchReportUseCase {
  private readonly repository = inject(ReportRepository);

  execute(criteria: ReportSearchCriteria): Observable<ReportPage> {
    return this.repository.search(criteria);
  }
}
