import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { MaterialRepository } from '../../catalogs/materials/domain/repositories/material.repository';

export interface DashboardSummary {
  materialsTotal: number;
}

/** Obtiene únicamente indicadores respaldados por endpoints existentes. */
@Injectable({ providedIn: 'root' })
export class DashboardSummaryService {
  private readonly materials = inject(MaterialRepository);

  resumen(): Observable<DashboardSummary> {
    return this.materials.search({ filter: '', page: 1, pageSize: 1 }).pipe(map((page) => ({ materialsTotal: page.total })));
  }
}
