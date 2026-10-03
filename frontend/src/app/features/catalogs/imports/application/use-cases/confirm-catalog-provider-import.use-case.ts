import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CatalogProviderImportConfirmation } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { CatalogImportRepository } from '@features/catalogs/imports/domain/repositories/catalog-import.repository';

@Injectable({ providedIn: 'root' })
export class ConfirmCatalogProviderImportUseCase {
  private readonly repository = inject(CatalogImportRepository);

  execute(cargaId: number): Observable<CatalogProviderImportConfirmation> {
    return this.repository.confirmProvider(cargaId);
  }
}