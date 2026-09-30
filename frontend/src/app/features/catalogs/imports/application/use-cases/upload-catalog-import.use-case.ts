import { inject, Injectable } from '@angular/core';
import { CatalogImportRepository } from '@features/catalogs/imports/domain/repositories/catalog-import.repository';
import { CatalogImportResponse, CatalogImportType } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UploadCatalogImportUseCase {
  private readonly repository = inject(CatalogImportRepository);

  execute(type: CatalogImportType, file: File): Observable<CatalogImportResponse> {
    return this.repository.upload(type, file);
  }
}
