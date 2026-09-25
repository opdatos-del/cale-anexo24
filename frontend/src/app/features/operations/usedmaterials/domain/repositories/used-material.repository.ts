import { Observable } from 'rxjs';
import { UsedMaterialPage, UsedMaterialSearchCriteria } from '@features/operations/usedmaterials/domain/models/used-material.model';

/** Puerto de consulta del histórico de materiales utilizados. */
export abstract class UsedMaterialRepository {
  abstract search(criteria: UsedMaterialSearchCriteria): Observable<UsedMaterialPage>;
}
