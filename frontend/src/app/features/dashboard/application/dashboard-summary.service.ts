import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';
import { AuthService } from '@core/auth/auth.service';
import { MaterialRepository } from '@features/catalogs/materials/domain/repositories/material.repository';
import { ProductRepository } from '@features/catalogs/products/domain/repositories/product.repository';
import { StructureRepository } from '@features/catalogs/structures/domain/repositories/structure.repository';

export interface DashboardSystemStatus {
  status: string;
  moduleCDatabase: string;
  applicationDatabase: string;
  degraded: boolean;
}

export interface DashboardSummary {
  materialsTotal: number | null;
  productsTotal: number | null;
  structuresTotal: number | null;
  system: DashboardSystemStatus;
}

/** Obtiene indicadores respaldados por endpoints existentes y respetando RBAC. */
@Injectable({ providedIn: 'root' })
export class DashboardSummaryService {
  private readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly materials = inject(MaterialRepository);
  private readonly products = inject(ProductRepository);
  private readonly structures = inject(StructureRepository);

  resumen(): Observable<DashboardSummary> {
    return forkJoin({ materialsTotal: this.totalMateriales(), productsTotal: this.totalProductos(), structuresTotal: this.totalEstructuras(), system: this.estadoSistema() });
  }

  private totalMateriales(): Observable<number | null> {
    if (!this.auth.hasPermission('MATERIALES_CONSULTAR')) return of(null);
    return this.materials.search({ filter: '', page: 1, pageSize: 1 }).pipe(map((page) => page.total), catchError(() => of(null)));
  }

  private totalProductos(): Observable<number | null> {
    if (!this.auth.hasPermission('PRODUCTOS_CONSULTAR')) return of(null);
    return this.products.search({ filter: '', page: 1, pageSize: 1 }).pipe(map((page) => page.total), catchError(() => of(null)));
  }

  private totalEstructuras(): Observable<number | null> {
    if (!this.auth.hasPermission('ESTRUCTURAS_CONSULTAR')) return of(null);
    return this.structures.search({ product: '', material: '', page: 1, pageSize: 1 }).pipe(map((page) => page.total), catchError(() => of(null)));
  }

  private estadoSistema(): Observable<DashboardSystemStatus> {
    return this.http.get<{ status: string; moduleCDatabase: string; applicationDatabase: string }>('/api/v1/system/status').pipe(
      map((status) => ({ ...status, degraded: status.status !== 'UP' || status.moduleCDatabase !== 'UP' || status.applicationDatabase !== 'UP' })),
      catchError(() => of({ status: 'DOWN', moduleCDatabase: 'DOWN', applicationDatabase: 'DOWN', degraded: true })),
    );
  }
}
