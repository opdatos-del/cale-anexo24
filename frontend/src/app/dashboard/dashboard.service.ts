import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { MaterialService } from '../catalogos/material.service';

export interface DashboardSummary {
  materialesTotal: number;
}

/** Obtiene únicamente indicadores respaldados por endpoints existentes. */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly materiales = inject(MaterialService);

  resumen(): Observable<DashboardSummary> {
    return this.materiales.listar('', 1, 1).pipe(map((pagina) => ({ materialesTotal: pagina.total })));
  }
}
