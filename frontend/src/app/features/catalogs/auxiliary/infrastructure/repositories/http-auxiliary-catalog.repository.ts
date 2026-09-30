import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { AuxiliaryCatalogRepository } from '@features/catalogs/auxiliary/domain/repositories/auxiliary-catalog.repository';
import { AuxiliaryPage, AuxiliarySearchCriteria } from '@features/catalogs/auxiliary/domain/models/auxiliary-catalog.model';
import { AuxiliaryCatalogApi } from '@features/catalogs/auxiliary/infrastructure/api/auxiliary-catalog.api';
import { AuxiliaryCatalogMapper } from '@features/catalogs/auxiliary/infrastructure/mappers/auxiliary-catalog.mapper';

/** Implementación HTTP del puerto de catálogos auxiliares. */
@Injectable()
export class HttpAuxiliaryCatalogRepository implements AuxiliaryCatalogRepository {
  private readonly api = inject(AuxiliaryCatalogApi);

  searchUnits(criteria: AuxiliarySearchCriteria): Observable<AuxiliaryPage> {
    return this.api.searchUnits(criteria.filter, criteria.page, criteria.pageSize).pipe(map(AuxiliaryCatalogMapper.units));
  }

  searchMaterialTypes(criteria: AuxiliarySearchCriteria): Observable<AuxiliaryPage> {
    return this.api.searchMaterialTypes(criteria.filter, criteria.page, criteria.pageSize).pipe(map(AuxiliaryCatalogMapper.materialTypes));
  }

  searchWarehouses(criteria: AuxiliarySearchCriteria): Observable<AuxiliaryPage> {
    return this.api.searchWarehouses(criteria.filter, criteria.page, criteria.pageSize).pipe(map(AuxiliaryCatalogMapper.warehouses));
  }

  searchCategories(criteria: AuxiliarySearchCriteria): Observable<AuxiliaryPage> {
    return this.api.searchCategories(criteria.filter, criteria.page, criteria.pageSize).pipe(map(AuxiliaryCatalogMapper.categories));
  }
}
