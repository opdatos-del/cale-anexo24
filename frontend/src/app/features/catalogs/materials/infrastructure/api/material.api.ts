import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { MaterialPageResponseDto } from '@features/catalogs/materials/infrastructure/dto/material-response.dto';

/** Adaptador HTTP: conoce únicamente el contrato REST de materiales. */
@Injectable({ providedIn: 'root' })
export class MaterialApi {
  private readonly http = inject(HttpClient);

  search(filter: string, page: number, pageSize: number): Observable<MaterialPageResponseDto> {
    let params = new HttpParams().set('pagina', page).set('tamano', pageSize);
    if (filter.trim()) params = params.set('filtro', filter.trim());
    return this.http.get<MaterialPageResponseDto>('/api/v1/catalogos/materiales', { params });
  }
}
