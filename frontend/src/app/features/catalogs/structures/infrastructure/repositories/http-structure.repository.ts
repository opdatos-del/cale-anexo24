import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { StructureApi } from '../api/structure.api';
import { StructureMapper } from '../mappers/structure.mapper';
import { StructurePage, StructureRepository, StructureSearchCriteria } from '../../domain/repositories/structure.repository';

/** Implementación HTTP del puerto de estructuras. */
@Injectable()
export class HttpStructureRepository implements StructureRepository {
  private readonly api = inject(StructureApi);

  search(criteria: StructureSearchCriteria): Observable<StructurePage> {
    return this.api.search(criteria.product, criteria.material, criteria.page, criteria.pageSize).pipe(map(StructureMapper.toPage));
  }
}
