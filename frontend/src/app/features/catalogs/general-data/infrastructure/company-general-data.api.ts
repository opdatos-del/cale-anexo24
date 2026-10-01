import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CompanyGeneralData } from '@features/catalogs/general-data/domain/company-general-data.model';

/** Consulta HTTP read-only de datos generales empresariales. */
@Injectable({ providedIn: 'root' })
export class CompanyGeneralDataApi {
  private readonly http = inject(HttpClient);

  get(): Observable<CompanyGeneralData | null> {
    return this.http.get<CompanyGeneralData | null>('/api/v1/catalogos/datos-generales');
  }
}
