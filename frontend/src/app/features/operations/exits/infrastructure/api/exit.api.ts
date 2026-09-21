import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { OperationSearchCriteria } from '@features/operations/shared/operation-search-criteria';
import { ExitPageResponseDto } from '../dto/exit-response.dto';

/** Adaptador HTTP del contrato real de Salidas. */
@Injectable({ providedIn: 'root' })
export class ExitApi {
  private readonly http = inject(HttpClient);

  search(criteria: OperationSearchCriteria): Observable<ExitPageResponseDto> {
    let params = new HttpParams()
      .set('desde', criteria.from)
      .set('hasta', criteria.to)
      .set('pagina', criteria.page)
      .set('tamano', criteria.pageSize);

    const optionalFilters: Record<string, string> = {
      pedimento: criteria.customsDocument,
      clavePedimento: criteria.customsCode,
      fraccion: criteria.tariffFraction,
      numeroParte: criteria.partNumber,
    };

    for (const [name, value] of Object.entries(optionalFilters)) {
      if (value.trim()) params = params.set(name, value.trim());
    }

    return this.http.get<ExitPageResponseDto>('/api/v1/operaciones/salidas', { params });
  }
}
