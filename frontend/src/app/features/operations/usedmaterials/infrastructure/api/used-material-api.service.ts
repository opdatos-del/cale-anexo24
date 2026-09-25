import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { UsedMaterialSearchCriteria } from '@features/operations/usedmaterials/domain/models/used-material.model';
import { UsedMaterialPageResponseDto } from './dto/used-material.dto';

/** Adaptador HTTP del contrato V1 de materiales utilizados. */
@Injectable({ providedIn: 'root' })
export class UsedMaterialApiService {
  private readonly http = inject(HttpClient);

  search(criteria: UsedMaterialSearchCriteria): Observable<UsedMaterialPageResponseDto> {
    let params = new HttpParams()
      .set('desde', criteria.from)
      .set('hasta', criteria.to)
      .set('pagina', criteria.page)
      .set('tamano', criteria.pageSize);

    const optionalFilters: Record<string, string> = {
      material: criteria.material,
      producto: criteria.product,
      pedimentoSalida: criteria.exitCustomsDocument,
      clavePedimentoSalida: criteria.exitCustomsCode,
    };

    for (const [name, value] of Object.entries(optionalFilters)) {
      if (value.trim()) params = params.set(name, value.trim());
    }

    return this.http.get<UsedMaterialPageResponseDto>('/api/v1/operaciones/materiales-utilizados', { params });
  }
}
