import { Observable } from 'rxjs';
import { StructureLine } from '../models/structure-line.model';

export interface StructureSearchCriteria {
  product: string;
  material: string;
  page: number;
  pageSize: number;
}

export interface StructurePage {
  items: StructureLine[];
  total: number;
  page: number;
  pageSize: number;
}

/** Puerto de consulta de estructuras y materiales asociados. */
export abstract class StructureRepository {
  abstract search(criteria: StructureSearchCriteria): Observable<StructurePage>;
}
