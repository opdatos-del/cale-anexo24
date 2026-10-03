import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CatalogProductImportConfirmation } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { CatalogImportRepository } from '@features/catalogs/imports/domain/repositories/catalog-import.repository';

@Injectable({ providedIn: 'root' })
export class ConfirmCatalogProductImportUseCase {
  private readonly repository = inject(CatalogImportRepository);

  execute(cargaId: number): Observable<CatalogProductImportConfirmation> {
    return this.repository.confirmProduct(cargaId);
  }
}