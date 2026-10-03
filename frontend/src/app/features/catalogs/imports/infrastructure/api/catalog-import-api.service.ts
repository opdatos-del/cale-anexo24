import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CatalogImportResponse, CatalogImportType, CatalogMaterialImportConfirmation, CatalogProductImportConfirmation, CatalogClientImportConfirmation } from '@features/catalogs/imports/domain/models/catalog-import.model';

@Injectable({ providedIn: 'root' })
export class CatalogImportApiService {
  private readonly http = inject(HttpClient);

  upload(type: CatalogImportType, file: File): Observable<CatalogImportResponse> {
    const form = new FormData();
    form.append('archivo', file);
    const endpoint = type === 'MATERIAL' ? 'materiales' : type === 'PRODUCTO' ? 'productos' : 'clientes';
    return this.http.post<CatalogImportResponse>(`/api/v1/catalogos/importaciones/${endpoint}`, form);
  }

  get(type: CatalogImportType, id: number): Observable<CatalogImportResponse> {
    const endpoint = type === 'MATERIAL' ? 'materiales' : type === 'PRODUCTO' ? 'productos' : 'clientes';
    return this.http.get<CatalogImportResponse>(`/api/v1/catalogos/importaciones/${endpoint}/${id}`);
  }

  confirmMaterial(cargaId: number): Observable<CatalogMaterialImportConfirmation> {
    return this.http.post<CatalogMaterialImportConfirmation>(`/api/v1/catalogos/importaciones/materiales/${cargaId}/confirmacion`, {});
  }

  confirmProduct(cargaId: number): Observable<CatalogProductImportConfirmation> {
    return this.http.post<CatalogProductImportConfirmation>(`/api/v1/catalogos/importaciones/productos/${cargaId}/confirmacion`, {});
  }

  confirmClient(cargaId: number): Observable<CatalogClientImportConfirmation> {
    return this.http.post<CatalogClientImportConfirmation>(`/api/v1/catalogos/importaciones/clientes/${cargaId}/confirmacion`, {});
  }
}
