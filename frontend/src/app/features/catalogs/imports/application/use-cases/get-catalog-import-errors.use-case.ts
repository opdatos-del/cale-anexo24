import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CatalogImportError, CatalogImportType } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { CatalogImportRepository } from '@features/catalogs/imports/domain/repositories/catalog-import.repository';

@Injectable({ providedIn: 'root' })
export class GetCatalogImportErrorsUseCase {
  private readonly repository = inject(CatalogImportRepository);

  execute(type: CatalogImportType, cargaId: number, pagina: number, tamano: number): Observable<CatalogImportError[]> {
    return this.repository.getErrors(type, cargaId, pagina, tamano);
  }
}
