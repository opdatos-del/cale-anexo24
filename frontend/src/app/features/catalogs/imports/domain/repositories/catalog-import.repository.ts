import { Observable } from 'rxjs';
import { CatalogImportResponse, CatalogImportType, CatalogMaterialImportConfirmation, CatalogProductImportConfirmation, CatalogClientImportConfirmation, CatalogProviderImportConfirmation, CatalogAgentImportConfirmation, CatalogSubmaquilaImportConfirmation } from '@features/catalogs/imports/domain/models/catalog-import.model';

export abstract class CatalogImportRepository {
  abstract upload(type: CatalogImportType, file: File): Observable<CatalogImportResponse>;
  abstract get(type: CatalogImportType, id: number): Observable<CatalogImportResponse>;
  abstract confirmMaterial(cargaId: number): Observable<CatalogMaterialImportConfirmation>;
  abstract confirmProduct(cargaId: number): Observable<CatalogProductImportConfirmation>;
  abstract confirmClient(cargaId: number): Observable<CatalogClientImportConfirmation>;
  abstract confirmProvider(cargaId: number): Observable<CatalogProviderImportConfirmation>;
  abstract confirmAgent(cargaId: number): Observable<CatalogAgentImportConfirmation>;
  abstract confirmSubmaquila(cargaId: number): Observable<CatalogSubmaquilaImportConfirmation>;
}
