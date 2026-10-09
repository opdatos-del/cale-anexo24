import { Observable } from 'rxjs';
import { CatalogImportResponse, CatalogImportError, CatalogImportType, CatalogMaterialImportConfirmation, CatalogProductImportConfirmation, CatalogClientImportConfirmation, CatalogProviderImportConfirmation, CatalogAgentImportConfirmation } from '@features/catalogs/imports/domain/models/catalog-import.model';

export abstract class CatalogImportRepository {
  abstract upload(type: CatalogImportType, file: File): Observable<CatalogImportResponse>;
  abstract get(type: CatalogImportType, id: number, pagina: number, tamano: number): Observable<CatalogImportResponse>;
  abstract getErrors(type: CatalogImportType, id: number, pagina: number, tamano: number): Observable<CatalogImportError[]>;
  abstract confirmMaterial(cargaId: number): Observable<CatalogMaterialImportConfirmation>;
  abstract confirmProduct(cargaId: number): Observable<CatalogProductImportConfirmation>;
  abstract confirmClient(cargaId: number): Observable<CatalogClientImportConfirmation>;
  abstract confirmProvider(cargaId: number): Observable<CatalogProviderImportConfirmation>;
  abstract confirmAgent(cargaId: number): Observable<CatalogAgentImportConfirmation>;
}
