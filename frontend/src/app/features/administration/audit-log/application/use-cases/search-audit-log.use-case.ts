import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuditLogPage, AuditLogSearchCriteria } from '../../domain/models/audit-log.model';
import { AuditLogRepository } from '../../domain/repositories/audit-log.repository';

/** Caso de uso de consulta paginada read-only de Bitácora. */
@Injectable({ providedIn: 'root' })
export class SearchAuditLogUseCase {
  private readonly repository = inject(AuditLogRepository);

  execute(criteria: AuditLogSearchCriteria): Observable<AuditLogPage> {
    return this.repository.search(criteria);
  }
}
