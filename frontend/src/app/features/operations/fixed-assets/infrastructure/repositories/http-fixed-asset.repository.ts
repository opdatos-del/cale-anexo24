import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { FixedAssetPage, FixedAssetSearchCriteria } from '@features/operations/fixed-assets/domain/models/fixed-asset.model';
import { FixedAssetRepository } from '@features/operations/fixed-assets/domain/repositories/fixed-asset.repository';
import { FixedAssetApiService } from '../api/fixed-asset-api.service';
import { FixedAssetMapper } from '../api/mappers/fixed-asset.mapper';

/** Implementación HTTP del puerto de activos fijos. */
@Injectable()
export class HttpFixedAssetRepository implements FixedAssetRepository {
  private readonly api = inject(FixedAssetApiService);

  search(criteria: FixedAssetSearchCriteria): Observable<FixedAssetPage> {
    return this.api.search(criteria).pipe(map(FixedAssetMapper.toPage));
  }
}
