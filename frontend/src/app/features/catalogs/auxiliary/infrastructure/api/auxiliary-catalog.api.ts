import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AlmacenResponseDto,
  AuxiliaryPageResponseDto,
  CategoriaResponseDto,
  TipoMaterialResponseDto,
  UnidadResponseDto,
} from '@features/catalogs/auxiliary/domain/models/auxiliary-catalog.model';

/** Contratos HTTP explícitos de los cuatro catálogos auxiliares V1. */
@Injectable({ providedIn: 'root' })
export class AuxiliaryCatalogApi {
  private readonly http = inject(HttpClient);

  searchUnits(filter: string, page: number, pageSize: number): Observable<AuxiliaryPageResponseDto<UnidadResponseDto>> {
    return this.http.get<AuxiliaryPageResponseDto<UnidadResponseDto>>('/api/v1/catalogos/unidades', { params: this.params(filter, page, pageSize) });
  }

  searchMaterialTypes(filter: string, page: number, pageSize: number): Observable<AuxiliaryPageResponseDto<TipoMaterialResponseDto>> {
    return this.http.get<AuxiliaryPageResponseDto<TipoMaterialResponseDto>>('/api/v1/catalogos/tipos-material', { params: this.params(filter, page, pageSize) });
  }

  searchWarehouses(filter: string, page: number, pageSize: number): Observable<AuxiliaryPageResponseDto<AlmacenResponseDto>> {
    return this.http.get<AuxiliaryPageResponseDto<AlmacenResponseDto>>('/api/v1/catalogos/almacenes', { params: this.params(filter, page, pageSize) });
  }

  searchCategories(filter: string, page: number, pageSize: number): Observable<AuxiliaryPageResponseDto<CategoriaResponseDto>> {
    return this.http.get<AuxiliaryPageResponseDto<CategoriaResponseDto>>('/api/v1/catalogos/categorias', { params: this.params(filter, page, pageSize) });
  }

  private params(filter: string, page: number, pageSize: number): HttpParams {
    const params = new HttpParams().set('pagina', page).set('tamano', pageSize);
    return filter.trim() ? params.set('filtro', filter.trim()) : params;
  }
}
