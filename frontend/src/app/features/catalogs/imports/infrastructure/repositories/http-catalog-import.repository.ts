import { Injectable, inject } from '@angular/core';
import { CatalogImportApiService } from '@features/catalogs/imports/infrastructure/api/catalog-import-api.service';
import { CatalogImportRepository } from '@features/catalogs/imports/domain/repositories/catalog-import.repository';
import { CatalogImportResponse, CatalogImportType } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { Observable } from 'rxjs';

@Injectable()
export class HttpCatalogImportRepository extends CatalogImportRepository {
  private readonly api = inject(CatalogImportApiService);

  upload(type: CatalogImportType, file: File): Observable<CatalogImportResponse> {
    return this.api.upload(type, file);
  }

  get(type: CatalogImportType, id: number): Observable<CatalogImportResponse> {
    return this.api.get(type, id);
  }
}
