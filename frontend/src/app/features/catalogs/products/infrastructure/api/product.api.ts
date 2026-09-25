import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProductPageResponseDto } from '@features/catalogs/products/infrastructure/dto/product-response.dto';

/** Adaptador HTTP del contrato real de Productos. */
@Injectable({ providedIn: 'root' })
export class ProductApi {
  private readonly http = inject(HttpClient);

  search(filter: string, page: number, pageSize: number): Observable<ProductPageResponseDto> {
    let params = new HttpParams().set('pagina', page).set('tamano', pageSize);
    if (filter.trim()) params = params.set('filtro', filter.trim());
    return this.http.get<ProductPageResponseDto>('/api/v1/catalogos/productos', { params });
  }
}
