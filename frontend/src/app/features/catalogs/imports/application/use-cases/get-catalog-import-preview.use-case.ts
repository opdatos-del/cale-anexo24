import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CatalogImportResponse, CatalogImportType } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { CatalogImportRepository } from '@features/catalogs/imports/domain/repositories/catalog-import.repository';

@Injectable({ providedIn: 'root' })
export class GetCatalogImportPreviewUseCase {
  private readonly repository = inject(CatalogImportRepository);

  execute(type: CatalogImportType, cargaId: number, pagina: number, tamano: number): Observable<CatalogImportResponse> {
    return this.repository.get(type, cargaId, pagina, tamano);
  }
}
