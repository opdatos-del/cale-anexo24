import { Observable } from 'rxjs';
import { Product } from '@features/catalogs/products/domain/models/product.model';

export interface ProductSearchCriteria {
  filter: string;
  page: number;
  pageSize: number;
}

export interface ProductPage {
  items: Product[];
  total: number;
  page: number;
  pageSize: number;
}

/** Puerto de consulta del catálogo de productos. */
export abstract class ProductRepository {
  abstract search(criteria: ProductSearchCriteria): Observable<ProductPage>;
}
