import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { StructurePageResponseDto } from '../dto/structure-response.dto';

/** Adaptador HTTP del contrato real de Estructuras. */
@Injectable({ providedIn: 'root' })
export class StructureApi {
  private readonly http = inject(HttpClient);

  search(product: string, material: string, page: number, pageSize: number): Observable<StructurePageResponseDto> {
    let params = new HttpParams().set('pagina', page).set('tamano', pageSize);
    if (product.trim()) params = params.set('producto', product.trim());
    if (material.trim()) params = params.set('material', material.trim());
    return this.http.get<StructurePageResponseDto>('/api/v1/catalogos/estructuras', { params });
  }
}
