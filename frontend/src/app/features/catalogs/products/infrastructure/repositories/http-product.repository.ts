import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ProductApi } from '@features/catalogs/products/infrastructure/api/product.api';
import { ProductMapper } from '@features/catalogs/products/infrastructure/mappers/product.mapper';
import { ProductPage, ProductRepository, ProductSearchCriteria } from '@features/catalogs/products/domain/repositories/product.repository';

/** Implementación HTTP del puerto de productos. */
@Injectable()
export class HttpProductRepository implements ProductRepository {
  private readonly api = inject(ProductApi);

  search(criteria: ProductSearchCriteria): Observable<ProductPage> {
    return this.api.search(criteria.filter, criteria.page, criteria.pageSize).pipe(map(ProductMapper.toPage));
  }
}
