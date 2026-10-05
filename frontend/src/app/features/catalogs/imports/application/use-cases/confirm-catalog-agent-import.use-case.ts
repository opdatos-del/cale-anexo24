import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CatalogAgentImportConfirmation } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { CatalogImportRepository } from '@features/catalogs/imports/domain/repositories/catalog-import.repository';

@Injectable({ providedIn: 'root' })
export class ConfirmCatalogAgentImportUseCase {
  private readonly repository = inject(CatalogImportRepository);

  execute(cargaId: number): Observable<CatalogAgentImportConfirmation> {
    return this.repository.confirmAgent(cargaId);
  }
}
