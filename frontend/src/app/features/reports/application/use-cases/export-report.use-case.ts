import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ReportSearchCriteria } from '@features/reports/domain/models/report.model';
import { ReportRepository } from '@features/reports/domain/repositories/report.repository';

/** Caso de uso para exportar el reporte seleccionado a XLSX. */
@Injectable({ providedIn: 'root' })
export class ExportReportUseCase {
  private readonly repository = inject(ReportRepository);

  execute(criteria: ReportSearchCriteria): Observable<Blob> {
    return this.repository.export(criteria);
  }
}
