import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuditLogSearchCriteria } from '../../domain/models/audit-log.model';
import { AuditLogPageResponseDto } from './dto/audit-log.dto';

/** Adaptador HTTP del contrato V1 de Bitácora. */
@Injectable({ providedIn: 'root' })
export class AuditLogApiService {
  private readonly http = inject(HttpClient);

  search(criteria: AuditLogSearchCriteria): Observable<AuditLogPageResponseDto> {
    let params = new HttpParams()
      .set('desde', criteria.from)
      .set('hasta', criteria.to)
      .set('pagina', criteria.page)
      .set('tamano', criteria.pageSize);

    if (criteria.userId !== null) {
      params = params.set('usuarioId', criteria.userId);
    }
    if (criteria.module) {
      params = params.set('modulo', criteria.module);
    }
    if (criteria.result) {
      params = params.set('resultado', criteria.result);
    }

    const correlationId = criteria.correlationId.trim();
    if (correlationId) {
      params = params.set('correlationId', correlationId);
    }

    return this.http.get<AuditLogPageResponseDto>('/api/v1/bitacora', { params });
  }
}
