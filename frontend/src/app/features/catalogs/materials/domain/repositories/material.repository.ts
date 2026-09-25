import { Observable } from 'rxjs';
import { Material } from '@features/catalogs/materials/domain/models/material.model';

export interface MaterialSearchCriteria {
  filter: string;
  page: number;
  pageSize: number;
}

export interface MaterialPage {
  items: Material[];
  total: number;
  page: number;
  pageSize: number;
}

/** Puerto de consulta del catálogo de materiales. */
export abstract class MaterialRepository {
  abstract search(criteria: MaterialSearchCriteria): Observable<MaterialPage>;
}
