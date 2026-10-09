import { Injectable, inject } from '@angular/core';
import { CatalogImportApiService } from '@features/catalogs/imports/infrastructure/api/catalog-import-api.service';
import { CatalogImportRepository } from '@features/catalogs/imports/domain/repositories/catalog-import.repository';
import { CatalogImportResponse, CatalogImportError, CatalogImportType, CatalogMaterialImportConfirmation, CatalogProductImportConfirmation, CatalogClientImportConfirmation, CatalogProviderImportConfirmation, CatalogAgentImportConfirmation } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { Observable } from 'rxjs';

@Injectable()
export class HttpCatalogImportRepository extends CatalogImportRepository {
  private readonly api = inject(CatalogImportApiService);

  upload(type: CatalogImportType, file: File): Observable<CatalogImportResponse> {
    return this.api.upload(type, file);
  }

  get(type: CatalogImportType, id: number, pagina: number, tamano: number): Observable<CatalogImportResponse> {
    return this.api.get(type, id, pagina, tamano);
  }

  getErrors(type: CatalogImportType, id: number, pagina: number, tamano: number): Observable<CatalogImportError[]> {
    return this.api.getErrors(type, id, pagina, tamano);
  }

  confirmMaterial(cargaId: number): Observable<CatalogMaterialImportConfirmation> {
    return this.api.confirmMaterial(cargaId);
  }

  confirmProduct(cargaId: number): Observable<CatalogProductImportConfirmation> {
    return this.api.confirmProduct(cargaId);
  }

  confirmClient(cargaId: number): Observable<CatalogClientImportConfirmation> {
    return this.api.confirmClient(cargaId);
  }

  confirmProvider(cargaId: number): Observable<CatalogProviderImportConfirmation> {
    return this.api.confirmProvider(cargaId);
  }

  confirmAgent(cargaId: number): Observable<CatalogAgentImportConfirmation> {
    return this.api.confirmAgent(cargaId);
  }
}
