import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { FixedAssetPage, FixedAssetSearchCriteria } from '../../domain/models/fixed-asset.model';
import { FixedAssetRepository } from '../../domain/repositories/fixed-asset.repository';

/** Caso de uso de consulta paginada de activos fijos. */
@Injectable({ providedIn: 'root' })
export class SearchFixedAssetsUseCase {
  private readonly repository = inject(FixedAssetRepository);

  execute(criteria: FixedAssetSearchCriteria): Observable<FixedAssetPage> {
    return this.repository.search(criteria);
  }
}
