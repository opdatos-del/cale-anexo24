import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { userFacingApiError } from '../../../../../../core/http/api-error.util';
import { NotificationService } from '../../../../../../core/notifications/notification.service';
import { AppAlertComponent } from '../../../../../../core/ui/app-alert/app-alert.component';
import { StructureLine } from '../../../domain/models/structure-line.model';
import { SearchStructuresUseCase } from '../../../application/use-cases/search-structures.use-case';

/** Consulta de estructuras y materiales asociados a productos. */
@Component({
  imports: [AppAlertComponent, FormsModule, MatButtonModule, MatIconModule, MatInputModule, MatPaginatorModule, MatTableModule],
  selector: 'app-structure-list',
  template: `
    <div class="min-h-full bg-[#f4f7fb] text-slate-800">
      <main class="mx-auto w-full max-w-360 px-5 py-8 sm:px-8">
        <header class="mb-7">
          <p class="mb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Catálogos</p>
          <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Estructuras</h1>
          <p class="mt-2 text-sm text-slate-500">Consulta de estructuras y materiales asociados a productos.</p>
        </header>

        <section class="mb-6 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-labelledby="structure-filters">
          <h2 id="structure-filters" class="sr-only">Filtros de estructuras</h2>
          <form class="grid grid-cols-1 items-end gap-4 md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_auto_auto]" (ngSubmit)="search()">
            <label>
              <span class="mb-1 block text-xs font-medium text-slate-700">Código de producto</span>
              <input matInput name="product" [(ngModel)]="product" placeholder="Ej. PT-001" class="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-4 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
            </label>
            <label>
              <span class="mb-1 block text-xs font-medium text-slate-700">Código de material</span>
              <input matInput name="material" [(ngModel)]="material" placeholder="Ej. MAT-001" class="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-4 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
            </label>
            <button mat-flat-button color="primary" type="submit" class="h-11 min-w-28 rounded-xl!">Consultar</button>
            <button mat-stroked-button type="button" class="h-11 min-w-24 rounded-xl!" (click)="clearFilters()">Limpiar</button>
          </form>
        </section>

        <div class="mb-4 flex flex-wrap items-center gap-3">
          <button mat-stroked-button type="button" class="h-10 rounded-xl!" (click)="loadStructures()" aria-label="Actualizar catálogo de estructuras">
            <mat-icon>refresh</mat-icon> Actualizar
          </button>
          <span class="text-xs text-slate-500" aria-live="polite">{{ totalItems() }} registros encontrados</span>
        </div>

        @if (isLoading()) {
          <div class="space-y-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm" aria-label="Cargando estructuras" aria-busy="true">
            @for (row of loadingRows; track row) { <div class="h-10 animate-pulse rounded-lg bg-slate-100"></div> }
          </div>
        } @else if (error()) {
          <app-alert kind="error" title="No pudimos cargar las estructuras" [message]="error()!" actionLabel="Reintentar" (action)="loadStructures()" />
        } @else {
          <section class="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-label="Resultados de estructuras">
            @if (items().length > 0) {
              <div class="overflow-x-auto">
                <table mat-table [dataSource]="items()" class="w-full min-w-280" aria-label="Catálogo de estructuras">
                  <ng-container matColumnDef="productCode">
                    <th mat-header-cell *matHeaderCellDef class="w-40">Producto</th>
                    <td mat-cell *matCellDef="let line" class="font-medium text-slate-700">{{ line.productCode }}</td>
                  </ng-container>
                  <ng-container matColumnDef="productDescription">
                    <th mat-header-cell *matHeaderCellDef>Descripción producto</th>
                    <td mat-cell *matCellDef="let line" class="max-w-56">{{ line.productDescription }}</td>
                  </ng-container>
                  <ng-container matColumnDef="materialCode">
                    <th mat-header-cell *matHeaderCellDef class="w-40">Material</th>
                    <td mat-cell *matCellDef="let line" class="font-medium text-slate-700">{{ line.materialCode }}</td>
                  </ng-container>
                  <ng-container matColumnDef="materialDescription">
                    <th mat-header-cell *matHeaderCellDef>Descripción material</th>
                    <td mat-cell *matCellDef="let line" class="max-w-56">{{ line.materialDescription }}</td>
                  </ng-container>
                  <ng-container matColumnDef="materialTariffFraction">
                    <th mat-header-cell *matHeaderCellDef class="w-36">Fracción material</th>
                    <td mat-cell *matCellDef="let line">{{ line.materialTariffFraction }}</td>
                  </ng-container>
                  <ng-container matColumnDef="incorporatedQuantity">
                    <th mat-header-cell *matHeaderCellDef class="w-32">Incorporada</th>
                    <td mat-cell *matCellDef="let line" class="text-right tabular-nums">{{ formatQuantity(line.incorporatedQuantity) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="wasteQuantity">
                    <th mat-header-cell *matHeaderCellDef class="w-28">Merma</th>
                    <td mat-cell *matCellDef="let line" class="text-right tabular-nums">{{ formatQuantity(line.wasteQuantity) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="scrapQuantity">
                    <th mat-header-cell *matHeaderCellDef class="w-28">Desperdicio</th>
                    <td mat-cell *matCellDef="let line" class="text-right tabular-nums">{{ formatQuantity(line.scrapQuantity) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="startDate">
                    <th mat-header-cell *matHeaderCellDef class="w-32">Inicio</th>
                    <td mat-cell *matCellDef="let line">{{ formatDate(line.startDate) }}</td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
                </table>
              </div>
            } @else {
              <div class="p-4">
                <app-alert kind="info" title="No encontramos estructuras" message="Prueba con otro producto o material, o limpia los filtros." [actionLabel]="product || material ? 'Limpiar filtros' : ''" (action)="clearFilters()" />
              </div>
            }
            <mat-paginator [length]="totalItems()" [pageSize]="pageSize" [pageSizeOptions]="[20, 50, 100]" (page)="changePage($event)" showFirstLastButtons aria-label="Paginación de estructuras" />
          </section>
        }
      </main>
    </div>
  `,
})
export class StructureListPage implements OnInit {
  protected readonly displayedColumns = ['productCode', 'productDescription', 'materialCode', 'materialDescription', 'materialTariffFraction', 'incorporatedQuantity', 'wasteQuantity', 'scrapQuantity', 'startDate'];
  protected readonly items = signal<StructureLine[]>([]);
  protected readonly totalItems = signal(0);
  protected readonly isLoading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly loadingRows = [1, 2, 3, 4, 5];
  protected product = '';
  protected material = '';
  protected currentPage = 1;
  protected pageSize = 20;

  private requestSequence = 0;
  private readonly searchStructures = inject(SearchStructuresUseCase);
  private readonly notifications = inject(NotificationService);

  ngOnInit(): void {
    this.loadStructures();
  }

  protected search(): void {
    this.currentPage = 1;
    this.loadStructures();
  }

  protected clearFilters(): void {
    this.product = '';
    this.material = '';
    this.search();
  }

  protected changePage(event: PageEvent): void {
    this.currentPage = event.pageIndex + 1;
    this.pageSize = event.pageSize;
    this.loadStructures();
  }

  protected formatDate(value: string | null): string {
    if (!value) return '—';
    const date = value.split('T')[0];
    const [year, month, day] = date.split('-');
    return year && month && day ? `${day}/${month}/${year}` : value;
  }

  protected formatQuantity(value: number | string): string {
    if (typeof value === 'string') return value;
    return new Intl.NumberFormat('es-MX', { maximumFractionDigits: 20 }).format(value);
  }

  protected loadStructures(): void {
    const requestId = ++this.requestSequence;
    this.isLoading.set(true);
    this.error.set(null);
    this.items.set([]);
    this.searchStructures.execute({ product: this.product, material: this.material, page: this.currentPage, pageSize: this.pageSize }).subscribe({
      next: (page) => {
        if (requestId !== this.requestSequence) return;
        this.items.set(page.items);
        this.totalItems.set(page.total);
        this.isLoading.set(false);
      },
      error: (error: unknown) => {
        if (requestId !== this.requestSequence) return;
        this.isLoading.set(false);
        this.error.set(userFacingApiError(error, 'Verifica tu conexión e inténtalo nuevamente.'));
        this.notifications.error('No fue posible consultar el catálogo de estructuras.');
      },
    });
  }
}
