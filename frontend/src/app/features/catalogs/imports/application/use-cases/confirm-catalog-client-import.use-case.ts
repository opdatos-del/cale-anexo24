import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CatalogClientImportConfirmation } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { CatalogImportRepository } from '@features/catalogs/imports/domain/repositories/catalog-import.repository';

@Injectable({ providedIn: 'root' })
export class ConfirmCatalogClientImportUseCase {
  private readonly repository = inject(CatalogImportRepository);

  execute(cargaId: number): Observable<CatalogClientImportConfirmation> {
    return this.repository.confirmClient(cargaId);
  }
}