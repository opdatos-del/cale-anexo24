import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ReportRow, ReportSearchCriteria } from '@features/reports/domain/models/report.model';

interface ReportPageResponseDto {
  items: ReportRow[];
  total: number;
  pagina: number;
  tamano: number;
}

/** Adaptador HTTP del contrato V1 de Reportes. */
@Injectable({ providedIn: 'root' })
export class ReportApiService {
  private readonly http = inject(HttpClient);

  search(criteria: ReportSearchCriteria): Observable<ReportPageResponseDto> {
    return this.http.get<ReportPageResponseDto>(this.url(criteria), { params: this.params(criteria) });
  }

  export(criteria: ReportSearchCriteria): Observable<Blob> {
    return this.http.get(`${this.url(criteria)}/exportacion`, { params: this.params(criteria, 100), responseType: 'blob' });
  }

  private url(criteria: ReportSearchCriteria): string {
    return `/api/v1/reportes/${criteria.type}`;
  }

  private params(criteria: ReportSearchCriteria, pageSize = criteria.pageSize): HttpParams {
    let params = new HttpParams()
      .set('desde', criteria.type === 'bitacora' ? this.startOfDayInstant(criteria.from) : criteria.from)
      .set('hasta', criteria.type === 'bitacora' ? this.endOfDayInstant(criteria.to) : criteria.to)
      .set('pagina', criteria.page)
      .set('tamano', pageSize);

    const optional = criteria.type === 'materiales-utilizados'
      ? { material: criteria.material, producto: criteria.product, pedimentoSalida: criteria.customsDocument, clavePedimentoSalida: criteria.customsCode }
      : criteria.type === 'bitacora'
        ? { usuarioId: criteria.userId?.toString() ?? '', modulo: criteria.module, resultado: criteria.result, correlationId: criteria.correlationId }
        : { pedimento: criteria.customsDocument, clavePedimento: criteria.customsCode, fraccion: criteria.tariffFraction, numeroParte: criteria.partNumber };

    for (const [name, value] of Object.entries(optional)) {
      if (value.trim()) params = params.set(name, value.trim());
    }
    return params;
  }

  private startOfDayInstant(date: string): string {
    return this.localInstant(date, 0, 0, 0, 0);
  }

  private endOfDayInstant(date: string): string {
    return this.localInstant(date, 23, 59, 59, 999);
  }

  private localInstant(date: string, hours: number, minutes: number, seconds: number, milliseconds: number): string {
    const [year, month, day] = date.split('-').map(Number);
    return new Date(year, month - 1, day, hours, minutes, seconds, milliseconds).toISOString();
  }
}
