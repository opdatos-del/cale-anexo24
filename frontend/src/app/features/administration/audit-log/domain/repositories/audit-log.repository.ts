import { Observable } from 'rxjs';
import { AuditLogPage, AuditLogSearchCriteria } from '@features/administration/audit-log/domain/models/audit-log.model';

/** Puerto de consulta read-only de Bitácora. */
export abstract class AuditLogRepository {
  abstract search(criteria: AuditLogSearchCriteria): Observable<AuditLogPage>;
}
