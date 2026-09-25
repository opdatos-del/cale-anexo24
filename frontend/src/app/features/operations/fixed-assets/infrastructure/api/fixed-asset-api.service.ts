import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { FixedAssetSearchCriteria } from '@features/operations/fixed-assets/domain/models/fixed-asset.model';
import { FixedAssetPageResponseDto } from './dto/fixed-asset.dto';

/** Adaptador HTTP del contrato V1 de activos fijos. */
@Injectable({ providedIn: 'root' })
export class FixedAssetApiService {
  private readonly http = inject(HttpClient);

  search(criteria: FixedAssetSearchCriteria): Observable<FixedAssetPageResponseDto> {
    let params = new HttpParams()
      .set('pagina', criteria.page)
      .set('tamano', criteria.pageSize);

    if (criteria.from && criteria.to) {
      params = params.set('desde', criteria.from).set('hasta', criteria.to);
    }

    const optionalFilters: Record<string, string> = {
      pedimento: criteria.customsDocument,
      clavePedimento: criteria.customsCode,
      numeroParte: criteria.partNumber,
      descripcion: criteria.description,
      serie: criteria.serialNumber,
      marca: criteria.brand,
      modelo: criteria.model,
    };

    for (const [name, value] of Object.entries(optionalFilters)) {
      if (value.trim()) params = params.set(name, value.trim());
    }

    return this.http.get<FixedAssetPageResponseDto>('/api/v1/operaciones/activos-fijos', { params });
  }
}
