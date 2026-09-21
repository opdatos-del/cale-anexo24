import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProductPage, ProductRepository, ProductSearchCriteria } from '../../domain/repositories/product.repository';

/** Caso de uso de consulta paginada del catálogo de productos. */
@Injectable({ providedIn: 'root' })
export class SearchProductsUseCase {
  private readonly repository = inject(ProductRepository);

  execute(criteria: ProductSearchCriteria): Observable<ProductPage> {
    return this.repository.search(criteria);
  }
}
