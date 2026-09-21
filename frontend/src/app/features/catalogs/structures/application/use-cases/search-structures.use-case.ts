import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { StructurePage, StructureRepository, StructureSearchCriteria } from '../../domain/repositories/structure.repository';

/** Caso de uso de consulta paginada de estructuras. */
@Injectable({ providedIn: 'root' })
export class SearchStructuresUseCase {
  private readonly repository = inject(StructureRepository);

  execute(criteria: StructureSearchCriteria): Observable<StructurePage> {
    return this.repository.search(criteria);
  }
}
