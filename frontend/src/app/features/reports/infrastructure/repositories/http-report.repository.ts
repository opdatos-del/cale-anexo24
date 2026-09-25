import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ReportPage, ReportSearchCriteria } from '@features/reports/domain/models/report.model';
import { ReportRepository } from '@features/reports/domain/repositories/report.repository';
import { ReportApiService } from '@features/reports/infrastructure/api/report-api.service';
import { ReportMapper } from '@features/reports/infrastructure/api/report.mapper';

/** Implementación HTTP del puerto de Reportes. */
@Injectable()
export class HttpReportRepository implements ReportRepository {
  private readonly api = inject(ReportApiService);

  search(criteria: ReportSearchCriteria): Observable<ReportPage> {
    return this.api.search(criteria).pipe(map(ReportMapper.toPage));
  }

  export(criteria: ReportSearchCriteria): Observable<Blob> {
    return this.api.export(criteria);
  }
}
