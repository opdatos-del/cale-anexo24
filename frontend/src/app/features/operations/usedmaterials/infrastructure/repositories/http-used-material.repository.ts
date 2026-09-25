import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { UsedMaterialPage, UsedMaterialSearchCriteria } from '@features/operations/usedmaterials/domain/models/used-material.model';
import { UsedMaterialRepository } from '@features/operations/usedmaterials/domain/repositories/used-material.repository';
import { UsedMaterialApiService } from '@features/operations/usedmaterials/infrastructure/api/used-material-api.service';
import { UsedMaterialMapper } from '@features/operations/usedmaterials/infrastructure/api/mappers/used-material.mapper';

/** Implementación HTTP del puerto de materiales utilizados. */
@Injectable()
export class HttpUsedMaterialRepository implements UsedMaterialRepository {
  private readonly api = inject(UsedMaterialApiService);

  search(criteria: UsedMaterialSearchCriteria): Observable<UsedMaterialPage> {
    return this.api.search(criteria).pipe(map(UsedMaterialMapper.toPage));
  }
}
