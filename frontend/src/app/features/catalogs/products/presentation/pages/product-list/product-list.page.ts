import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { userFacingApiError } from '@core/http/api-error.util';
import { NotificationService } from '@core/notifications/notification.service';
import { AppAlertComponent } from '@core/ui/app-alert/app-alert.component';
import { Product } from '@features/catalogs/products/domain/models/product.model';
import { SearchProductsUseCase } from '@features/catalogs/products/application/use-cases/search-products.use-case';

/** Consulta del catálogo de productos terminados. */
@Component({
  imports: [AppAlertComponent, FormsModule, MatButtonModule, MatIconModule, MatInputModule, MatPaginatorModule, MatTableModule],
  selector: 'app-product-list',
  template: `
    <div class="min-h-full bg-[#f4f7fb] text-slate-800">
      <main class="mx-auto w-full max-w-360 px-5 py-8 sm:px-8">
        <header class="mb-7">
          <p class="mb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Catálogos</p>
          <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Productos</h1>
          <p class="mt-2 text-sm text-slate-500">Consulta del catálogo de productos terminados.</p>
        </header>

        <section class="mb-6 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-labelledby="product-filters">
          <h2 id="product-filters" class="sr-only">Filtros de productos</h2>
          <form class="flex flex-wrap items-end gap-4" (ngSubmit)="search()">
            <label class="min-w-0 flex-1 sm:min-w-70">
              <span class="mb-1 block text-xs font-medium text-slate-700">Parte, descripción o fracción</span>
              <input matInput name="filter" [(ngModel)]="filter" placeholder="Buscar en el catálogo" class="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-4 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
            </label>
            <button mat-flat-button color="primary" type="submit" class="h-11 min-w-28 rounded-xl!">Consultar</button>
            <button mat-stroked-button type="button" class="h-11 min-w-24 rounded-xl!" (click)="clearFilter()">Limpiar</button>
          </form>
        </section>

        <div class="mb-4 flex flex-wrap items-center gap-3">
          <button mat-stroked-button type="button" class="h-10 rounded-xl!" (click)="loadProducts()" aria-label="Actualizar catálogo de productos">
            <mat-icon>refresh</mat-icon> Actualizar
          </button>
          <span class="text-xs text-slate-500" aria-live="polite">{{ totalItems() }} registros encontrados</span>
        </div>

        @if (isLoading()) {
          <div class="space-y-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm" aria-label="Cargando productos" aria-busy="true">
            @for (row of loadingRows; track row) { <div class="h-10 animate-pulse rounded-lg bg-slate-100"></div> }
          </div>
        } @else if (error()) {
          <app-alert kind="error" title="No pudimos cargar los productos" [message]="error()!" actionLabel="Reintentar" (action)="loadProducts()" />
        } @else {
          <section class="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-label="Resultados de productos">
            @if (items().length > 0) {
              <div class="overflow-x-auto">
                <table mat-table [dataSource]="items()" class="w-full min-w-190" aria-label="Catálogo de productos">
                  <ng-container matColumnDef="partNumber">
                    <th mat-header-cell *matHeaderCellDef class="w-40">N° parte</th>
                    <td mat-cell *matCellDef="let product" class="font-medium text-slate-700">{{ product.partNumber }}</td>
                  </ng-container>
                  <ng-container matColumnDef="description">
                    <th mat-header-cell *matHeaderCellDef>Descripción</th>
                    <td mat-cell *matCellDef="let product" class="max-w-140">{{ product.description }}</td>
                  </ng-container>
                  <ng-container matColumnDef="tariffFraction">
                    <th mat-header-cell *matHeaderCellDef class="w-36">Fracción</th>
                    <td mat-cell *matCellDef="let product">{{ product.tariffFraction }}</td>
                  </ng-container>
                  <ng-container matColumnDef="commercialUnit">
                    <th mat-header-cell *matHeaderCellDef class="w-28">UMC</th>
                    <td mat-cell *matCellDef="let product">{{ product.commercialUnit }}</td>
                  </ng-container>
                  <ng-container matColumnDef="tariffUnit">
                    <th mat-header-cell *matHeaderCellDef class="w-36">Unidad tarifaria</th>
                    <td mat-cell *matCellDef="let product">{{ product.tariffUnit }}</td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
                </table>
              </div>
            } @else {
              <div class="p-4">
                <app-alert kind="info" title="No encontramos productos" message="Prueba con otra búsqueda o limpia el filtro." [actionLabel]="filter ? 'Limpiar búsqueda' : ''" (action)="clearFilter()" />
              </div>
            }
            <mat-paginator [length]="totalItems()" [pageSize]="pageSize" [pageSizeOptions]="[20, 50, 100]" (page)="changePage($event)" showFirstLastButtons aria-label="Paginación de productos" />
          </section>
        }
      </main>
    </div>
  `,
})
export class ProductListPage implements OnInit {
  protected readonly displayedColumns = ['partNumber', 'description', 'tariffFraction', 'commercialUnit', 'tariffUnit'];
  protected readonly items = signal<Product[]>([]);
  protected readonly totalItems = signal(0);
  protected readonly isLoading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly loadingRows = [1, 2, 3, 4, 5];
  protected filter = '';
  protected currentPage = 1;
  protected pageSize = 20;

  private requestSequence = 0;
  private readonly searchProducts = inject(SearchProductsUseCase);
  private readonly notifications = inject(NotificationService);

  ngOnInit(): void {
    this.loadProducts();
  }

  protected search(): void {
    this.currentPage = 1;
    this.loadProducts();
  }

  protected clearFilter(): void {
    this.filter = '';
    this.search();
  }

  protected changePage(event: PageEvent): void {
    this.currentPage = event.pageIndex + 1;
    this.pageSize = event.pageSize;
    this.loadProducts();
  }

  protected loadProducts(): void {
    const requestId = ++this.requestSequence;
    this.isLoading.set(true);
    this.error.set(null);
    this.items.set([]);
    this.searchProducts.execute({ filter: this.filter, page: this.currentPage, pageSize: this.pageSize }).subscribe({
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
        this.notifications.error('No fue posible consultar el catálogo de productos.');
      },
    });
  }
}
