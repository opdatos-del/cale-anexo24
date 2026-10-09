import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CatalogImportResponse, CatalogImportError, CatalogImportType, CatalogMaterialImportConfirmation, CatalogProductImportConfirmation, CatalogClientImportConfirmation, CatalogProviderImportConfirmation, CatalogAgentImportConfirmation } from '@features/catalogs/imports/domain/models/catalog-import.model';

@Injectable({ providedIn: 'root' })
export class CatalogImportApiService {
  private readonly http = inject(HttpClient);

  upload(type: CatalogImportType, file: File): Observable<CatalogImportResponse> {
    const form = new FormData();
    form.append('archivo', file);
    const endpoint = this.endpoint(type);
    return this.http.post<CatalogImportResponse>(`/api/v1/catalogos/importaciones/${endpoint}`, form);
  }

  get(type: CatalogImportType, id: number, pagina = 1, tamano = 100): Observable<CatalogImportResponse> {
    const endpoint = this.endpoint(type);
    const params = new HttpParams().set('pagina', pagina).set('tamano', tamano);
    return this.http.get<CatalogImportResponse>('/api/v1/catalogos/importaciones/' + endpoint + '/' + id, { params });
  }

  getErrors(type: CatalogImportType, id: number, pagina = 1, tamano = 100): Observable<CatalogImportError[]> {
    const endpoint = this.endpoint(type);
    const params = new HttpParams().set('pagina', pagina).set('tamano', tamano);
    return this.http.get<CatalogImportError[]>('/api/v1/catalogos/importaciones/' + endpoint + '/' + id + '/errores', { params });
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

  confirmProvider(cargaId: number): Observable<CatalogProviderImportConfirmation> {
    return this.http.post<CatalogProviderImportConfirmation>(`/api/v1/catalogos/importaciones/proveedores/${cargaId}/confirmacion`, {});
  }

  confirmAgent(cargaId: number): Observable<CatalogAgentImportConfirmation> {
    return this.http.post<CatalogAgentImportConfirmation>(`/api/v1/catalogos/importaciones/agentes/${cargaId}/confirmacion`, {});
  }

  private endpoint(type: CatalogImportType): string {
    switch (type) {
      case 'MATERIAL': return 'materiales';
      case 'PRODUCTO': return 'productos';
      case 'CLIENTE': return 'clientes';
      case 'PROVEEDOR': return 'proveedores';
      case 'AGENTE': return 'agentes';
    }
  }
}
