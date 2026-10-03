import { Observable } from 'rxjs';
import { CatalogImportResponse, CatalogImportType, CatalogMaterialImportConfirmation } from '@features/catalogs/imports/domain/models/catalog-import.model';

export abstract class CatalogImportRepository {
  abstract upload(type: CatalogImportType, file: File): Observable<CatalogImportResponse>;
  abstract get(type: CatalogImportType, id: number): Observable<CatalogImportResponse>;
  abstract confirmMaterial(cargaId: number): Observable<CatalogMaterialImportConfirmation>;
}
