import { Observable } from 'rxjs';
import { FixedAssetPage, FixedAssetSearchCriteria } from '../models/fixed-asset.model';

/** Puerto de consulta paginada de activos fijos. */
export abstract class FixedAssetRepository {
  abstract search(criteria: FixedAssetSearchCriteria): Observable<FixedAssetPage>;
}
