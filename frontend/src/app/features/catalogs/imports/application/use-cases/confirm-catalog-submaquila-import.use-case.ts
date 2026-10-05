import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CatalogSubmaquilaImportConfirmation } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { CatalogImportRepository } from '@features/catalogs/imports/domain/repositories/catalog-import.repository';

@Injectable({ providedIn: 'root' })
export class ConfirmCatalogSubmaquilaImportUseCase {
  private readonly repository = inject(CatalogImportRepository);

  execute(cargaId: number): Observable<CatalogSubmaquilaImportConfirmation> {
    return this.repository.confirmSubmaquila(cargaId);
  }
}
