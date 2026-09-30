import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuxiliaryCatalogRepository } from '@features/catalogs/auxiliary/domain/repositories/auxiliary-catalog.repository';
import { AuxiliaryPage, AuxiliarySearchCriteria } from '@features/catalogs/auxiliary/domain/models/auxiliary-catalog.model';

/** Expone las consultas de cada catálogo sin permitir operaciones de escritura. */
@Injectable({ providedIn: 'root' })
export class SearchAuxiliaryCatalogUseCase {
  private readonly repository = inject(AuxiliaryCatalogRepository);

  units(criteria: AuxiliarySearchCriteria): Observable<AuxiliaryPage> {
    return this.repository.searchUnits(criteria);
  }

  materialTypes(criteria: AuxiliarySearchCriteria): Observable<AuxiliaryPage> {
    return this.repository.searchMaterialTypes(criteria);
  }

  warehouses(criteria: AuxiliarySearchCriteria): Observable<AuxiliaryPage> {
    return this.repository.searchWarehouses(criteria);
  }

  categories(criteria: AuxiliarySearchCriteria): Observable<AuxiliaryPage> {
    return this.repository.searchCategories(criteria);
  }
}
