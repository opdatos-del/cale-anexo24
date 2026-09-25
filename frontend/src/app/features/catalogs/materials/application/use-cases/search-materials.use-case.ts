import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { MaterialPage, MaterialRepository, MaterialSearchCriteria } from '@features/catalogs/materials/domain/repositories/material.repository';

/** Caso de uso de consulta paginada del catálogo. */
@Injectable({ providedIn: 'root' })
export class SearchMaterialsUseCase {
  private readonly repository = inject(MaterialRepository);

  execute(criteria: MaterialSearchCriteria): Observable<MaterialPage> {
    return this.repository.search(criteria);
  }
}
