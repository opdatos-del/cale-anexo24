import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { AuditLogPage, AuditLogSearchCriteria } from '@features/administration/audit-log/domain/models/audit-log.model';
import { AuditLogRepository } from '@features/administration/audit-log/domain/repositories/audit-log.repository';
import { AuditLogApiService } from '@features/administration/audit-log/infrastructure/api/audit-log-api.service';
import { AuditLogMapper } from '@features/administration/audit-log/infrastructure/api/mappers/audit-log.mapper';

/** Implementación HTTP del puerto read-only de Bitácora. */
@Injectable()
export class HttpAuditLogRepository implements AuditLogRepository {
  private readonly api = inject(AuditLogApiService);

  search(criteria: AuditLogSearchCriteria): Observable<AuditLogPage> {
    return this.api.search(criteria).pipe(map(AuditLogMapper.toPage));
  }
}
