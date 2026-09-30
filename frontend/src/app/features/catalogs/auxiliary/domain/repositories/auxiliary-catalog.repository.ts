import { Observable } from 'rxjs';
import { AuxiliaryPage, AuxiliarySearchCriteria } from '@features/catalogs/auxiliary/domain/models/auxiliary-catalog.model';

/** Puerto de consultas read-only de catálogos auxiliares confirmados. */
export abstract class AuxiliaryCatalogRepository {
  abstract searchUnits(criteria: AuxiliarySearchCriteria): Observable<AuxiliaryPage>;
  abstract searchMaterialTypes(criteria: AuxiliarySearchCriteria): Observable<AuxiliaryPage>;
  abstract searchWarehouses(criteria: AuxiliarySearchCriteria): Observable<AuxiliaryPage>;
  abstract searchCategories(criteria: AuxiliarySearchCriteria): Observable<AuxiliaryPage>;
}
