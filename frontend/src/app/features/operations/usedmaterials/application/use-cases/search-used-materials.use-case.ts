import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { UsedMaterialPage, UsedMaterialSearchCriteria } from '@features/operations/usedmaterials/domain/models/used-material.model';
import { UsedMaterialRepository } from '@features/operations/usedmaterials/domain/repositories/used-material.repository';

/** Caso de uso de consulta paginada de materiales utilizados. */
@Injectable({ providedIn: 'root' })
export class SearchUsedMaterialsUseCase {
  private readonly repository = inject(UsedMaterialRepository);

  execute(criteria: UsedMaterialSearchCriteria): Observable<UsedMaterialPage> {
    return this.repository.search(criteria);
  }
}
