import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { MaterialApi } from '../api/material.api';
import { MaterialMapper } from '../mappers/material.mapper';
import { MaterialPage, MaterialRepository, MaterialSearchCriteria } from '@features/catalogs/materials/domain/repositories/material.repository';

/** Implementación HTTP del puerto de materiales. */
@Injectable()
export class HttpMaterialRepository implements MaterialRepository {
  private readonly api = inject(MaterialApi);

  search(criteria: MaterialSearchCriteria): Observable<MaterialPage> {
    return this.api.search(criteria.filter, criteria.page, criteria.pageSize).pipe(map(MaterialMapper.toPage));
  }
}
