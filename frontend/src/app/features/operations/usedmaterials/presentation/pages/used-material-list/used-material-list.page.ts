import { Component, DestroyRef, ViewChild, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { EMPTY, Observable, Subject, catchError, debounce, filter, map, switchMap, timer } from 'rxjs';
import { userFacingApiError } from '@core/http/api-error.util';
import { NotificationService } from '@core/notifications/notification.service';
import { AppAlertComponent } from '@core/ui/app-alert/app-alert.component';
import {
  formatLocalDateForApi,
  formatOperationDate,
  formatOperationQuantity,
  formatOperationText,
} from '@features/operations/shared/operation-formatters';
import {
  OperationPeriod,
  OperationPeriodFilterComponent,
} from '@features/operations/shared/presentation/operation-period-filter/operation-period-filter.component';
import {
  UsedMaterial,
  UsedMaterialPage,
  UsedMaterialSearchCriteria,
} from '@features/operations/usedmaterials/domain/models/used-material.model';
import { SearchUsedMaterialsUseCase } from '@features/operations/usedmaterials/application/use-cases/search-used-materials.use-case';

interface UsedMaterialSearchResult {
  requestId: number;
  page: UsedMaterialPage;
}

/** Consulta paginada del histórico de materiales consumidos en las salidas. */
@Component({
  imports: [
    AppAlertComponent,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatTableModule,
    OperationPeriodFilterComponent,
  ],
  selector: 'app-used-material-list',
  template: `
    <div class="min-h-full bg-[#f4f7fb] text-slate-800">
      <main class="mx-auto w-full max-w-360 px-5 py-8 sm:px-8">
        <header class="mb-6">
          <p class="mb-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Operaciones</p>
          <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Materiales utilizados</h1>
          <p class="mt-1 text-sm text-slate-500">Consulta del histórico de materiales consumidos en las salidas.</p>
        </header>

        <section class="mb-4 rounded-2xl border border-slate-200/80 bg-white p-4 shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-labelledby="used-material-filters">
          <h2 id="used-material-filters" class="sr-only">Filtros de materiales utilizados</h2>
          <div class="flex flex-wrap items-center gap-3">
            <app-operation-period-filter #periodFilter class="min-w-0 flex-1" (periodChange)="onPeriodChange($event)" />
            <div class="flex shrink-0 items-center gap-1.5">
              <button
                mat-stroked-button
                type="button"
                class="h-9 rounded-lg! px-3!"
                [attr.aria-expanded]="optionalFiltersExpanded()"
                aria-controls="used-material-optional-filters"
                aria-label="Mostrar filtros opcionales"
                (click)="toggleOptionalFilters()"
              >
                <mat-icon class="text-[18px]!" aria-hidden="true">tune</mat-icon>
                <span>Filtros</span>
                @if (activeFilterCount() > 0) {
                  <span class="ml-1 inline-flex min-w-5 items-center justify-center rounded-full bg-blue-100 px-1.5 text-[11px] font-bold text-blue-700">{{ activeFilterCount() }}</span>
                }
              </button>
              <button mat-button type="button" class="h-9 rounded-lg! px-3! text-slate-500!" (click)="clearFilters()">Limpiar</button>
            </div>
          </div>

          @if (optionalFiltersExpanded()) {
            <div id="used-material-optional-filters" class="mt-3 grid grid-cols-1 gap-3 border-t border-slate-100 pt-3 md:grid-cols-2 xl:grid-cols-4">
              <label>
                <span class="mb-1 block text-xs font-medium text-slate-700">Material</span>
                <input id="used-material-code" matInput name="material" [(ngModel)]="material" (ngModelChange)="onOptionalFilterChange()" maxlength="50" placeholder="Código de material" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
              </label>
              <label>
                <span class="mb-1 block text-xs font-medium text-slate-700">Producto</span>
                <input id="used-material-product" matInput name="product" [(ngModel)]="product" (ngModelChange)="onOptionalFilterChange()" maxlength="50" placeholder="Código de producto" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
              </label>
              <label>
                <span class="mb-1 block text-xs font-medium text-slate-700">Pedimento de salida</span>
                <input id="used-material-exit-customs-document" matInput name="exitCustomsDocument" [(ngModel)]="exitCustomsDocument" (ngModelChange)="onOptionalFilterChange()" maxlength="50" placeholder="Documento de salida" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
              </label>
              <label>
                <span class="mb-1 block text-xs font-medium text-slate-700">Clave de pedimento</span>
                <input id="used-material-exit-customs-code" matInput name="exitCustomsCode" [(ngModel)]="exitCustomsCode" (ngModelChange)="onOptionalFilterChange()" maxlength="5" placeholder="Clave" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
              </label>
            </div>
          }
        </section>

        @if (hasSearched()) {
          <div class="mb-3 flex min-h-9 items-center justify-between gap-3 px-1">
            <span class="text-xs font-medium text-slate-500" aria-live="polite">{{ formatTotal() }} resultados</span>
            <button mat-button type="button" class="h-9 rounded-lg! px-3! text-slate-600!" (click)="refresh()" [disabled]="!canRefresh()" aria-label="Actualizar consulta de materiales utilizados">
              <mat-icon class="text-[18px]!" aria-hidden="true">refresh</mat-icon> Actualizar
            </button>
          </div>
        }

        @if (isLoading()) {
          <div class="space-y-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm" aria-label="Cargando materiales utilizados" aria-busy="true">
            @for (row of loadingRows; track row) { <div class="h-10 animate-pulse rounded-lg bg-slate-100"></div> }
          </div>
        } @else if (error()) {
          <section class="rounded-2xl border border-slate-200/80 bg-white p-4 shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-label="Error de consulta">
            <app-alert kind="error" title="No pudimos cargar los materiales utilizados" [message]="error()!" actionLabel="Reintentar" (action)="refresh()" />
          </section>
        } @else if (!hasSearched()) {
          <section class="flex min-h-52 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-10 text-center" aria-label="Estado inicial de materiales utilizados">
            <mat-icon class="mb-3 h-10 w-10 text-[40px]! text-slate-300" aria-hidden="true">calendar_month</mat-icon>
            <p class="m-0 text-sm font-medium text-slate-600">Selecciona un periodo</p>
            <span class="mt-1 text-xs text-slate-400">para consultar materiales utilizados</span>
          </section>
        } @else {
          <section class="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-label="Resultados de materiales utilizados">
            @if (items().length > 0) {
              <div class="overflow-x-auto">
                <table mat-table [dataSource]="items()" class="w-full min-w-290" aria-label="Materiales utilizados">
                  <ng-container matColumnDef="date">
                    <th mat-header-cell *matHeaderCellDef>Fecha</th>
                    <td mat-cell *matCellDef="let item">{{ formatDate(item.date) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="entryCustomsDocument">
                    <th mat-header-cell *matHeaderCellDef>Pedimento entrada</th>
                    <td mat-cell *matCellDef="let item" class="font-medium text-slate-700">{{ formatText(item.entryCustomsDocument) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="exitCustomsDocument">
                    <th mat-header-cell *matHeaderCellDef>Pedimento salida</th>
                    <td mat-cell *matCellDef="let item" class="font-medium text-slate-700">{{ formatText(item.exitCustomsDocument) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="material">
                    <th mat-header-cell *matHeaderCellDef>Material</th>
                    <td mat-cell *matCellDef="let item">
                      <div class="font-medium text-slate-700">{{ formatText(item.materialCode) }}</div>
                      <div class="mt-0.5 text-xs text-slate-400">{{ formatText(item.materialDescription) }}</div>
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="product">
                    <th mat-header-cell *matHeaderCellDef>Producto</th>
                    <td mat-cell *matCellDef="let item">
                      <div class="font-medium text-slate-700">{{ formatText(item.productCode) }}</div>
                      <div class="mt-0.5 text-xs text-slate-400">{{ formatText(item.productDescription) }}</div>
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="incorporatedQuantity">
                    <th mat-header-cell *matHeaderCellDef>Incorporada</th>
                    <td mat-cell *matCellDef="let item" class="text-right tabular-nums">{{ formatQuantity(item.incorporatedQuantity) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="wasteQuantity">
                    <th mat-header-cell *matHeaderCellDef>Merma</th>
                    <td mat-cell *matCellDef="let item" class="text-right tabular-nums">{{ formatQuantity(item.wasteQuantity) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="scrapQuantity">
                    <th mat-header-cell *matHeaderCellDef>Desperdicio</th>
                    <td mat-cell *matCellDef="let item" class="text-right tabular-nums">{{ formatQuantity(item.scrapQuantity) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="totalDischargedQuantity">
                    <th mat-header-cell *matHeaderCellDef>Total descargado</th>
                    <td mat-cell *matCellDef="let item" class="text-right font-medium tabular-nums text-slate-700">{{ formatQuantity(item.totalDischargedQuantity) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="unit">
                    <th mat-header-cell *matHeaderCellDef>Unidad</th>
                    <td mat-cell *matCellDef="let item">{{ formatText(item.unit) }}</td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
                </table>
              </div>
            } @else {
              <div class="flex min-h-52 flex-col items-center justify-center px-6 py-10 text-center" aria-label="Sin resultados">
                <mat-icon class="mb-3 h-10 w-10 text-[40px]! text-slate-300" aria-hidden="true">search_off</mat-icon>
                <p class="m-0 text-sm font-medium text-slate-600">No se encontraron materiales utilizados</p>
                <span class="mt-1 text-xs text-slate-400">No hay registros para el periodo y filtros seleccionados.</span>
              </div>
            }
            <mat-paginator [length]="totalItems()" [pageIndex]="currentPage - 1" [pageSize]="pageSize" [pageSizeOptions]="pageSizeOptions" (page)="changePage($event)" showFirstLastButtons aria-label="Paginación de materiales utilizados" />
          </section>
        }
      </main>
    </div>
  `,
})
export class UsedMaterialListPage {
  @ViewChild(OperationPeriodFilterComponent) private periodFilter?: OperationPeriodFilterComponent;

  protected readonly displayedColumns = [
    'date',
    'entryCustomsDocument',
    'exitCustomsDocument',
    'material',
    'product',
    'incorporatedQuantity',
    'wasteQuantity',
    'scrapQuantity',
    'totalDischargedQuantity',
    'unit',
  ];
  protected readonly pageSizeOptions = [20, 50, 100];
  protected readonly loadingRows = [1, 2, 3, 4, 5];
  protected readonly items = signal<UsedMaterial[]>([]);
  protected readonly totalItems = signal(0);
  protected readonly isLoading = signal(false);
  protected readonly hasSearched = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly optionalFiltersExpanded = signal(false);

  protected fromDate: Date | null = null;
  protected toDate: Date | null = null;
  protected material = '';
  protected product = '';
  protected exitCustomsDocument = '';
  protected exitCustomsCode = '';
  protected currentPage = 1;
  protected pageSize = 20;

  private requestSequence = 0;
  private readonly searchTriggers = new Subject<boolean>();
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchUsedMaterials = inject(SearchUsedMaterialsUseCase);
  private readonly notifications = inject(NotificationService);

  constructor() {
    this.searchTriggers
      .pipe(
        debounce((immediate) => timer(immediate ? 0 : 500)),
        map(() => this.buildCriteria()),
        filter(() => this.hasValidPeriod()),
        switchMap((criteria) => this.executeSearch(criteria)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ requestId, page }) => {
        if (requestId !== this.requestSequence) return;
        this.items.set(page.items);
        this.totalItems.set(page.total);
        this.isLoading.set(false);
      });
  }

  protected onPeriodChange(period: OperationPeriod): void {
    this.fromDate = period.start;
    this.toDate = period.end;

    if (!this.hasValidPeriod()) {
      this.invalidateResults();
      return;
    }

    this.currentPage = 1;
    this.hasSearched.set(true);
    this.searchTriggers.next(true);
  }

  protected onOptionalFilterChange(): void {
    this.optionalFiltersExpanded.set(true);
    if (!this.hasValidPeriod()) return;

    this.currentPage = 1;
    this.hasSearched.set(true);
    this.searchTriggers.next(false);
  }

  protected toggleOptionalFilters(): void {
    this.optionalFiltersExpanded.update((expanded) => !expanded);
  }

  protected activeFilterCount(): number {
    return [this.material, this.product, this.exitCustomsDocument, this.exitCustomsCode]
      .filter((value) => value.trim()).length;
  }

  protected formatTotal(): string {
    return new Intl.NumberFormat('es-MX').format(this.totalItems());
  }

  protected canRefresh(): boolean {
    return this.hasSearched() && this.hasValidPeriod();
  }

  protected refresh(): void {
    if (!this.canRefresh()) return;
    this.searchTriggers.next(true);
  }

  protected clearFilters(): void {
    this.requestSequence += 1;
    this.periodFilter?.clear();
    this.fromDate = null;
    this.toDate = null;
    this.material = '';
    this.product = '';
    this.exitCustomsDocument = '';
    this.exitCustomsCode = '';
    this.currentPage = 1;
    this.items.set([]);
    this.totalItems.set(0);
    this.error.set(null);
    this.hasSearched.set(false);
    this.isLoading.set(false);
    this.optionalFiltersExpanded.set(false);
  }

  protected changePage(event: PageEvent): void {
    if (!this.hasValidPeriod()) return;
    this.currentPage = event.pageIndex + 1;
    this.pageSize = event.pageSize;
    this.searchTriggers.next(true);
  }

  protected formatDate(value: string | null): string {
    return formatOperationDate(value);
  }

  protected formatQuantity(value: number | string | null): string {
    return formatOperationQuantity(value);
  }

  protected formatText(value: string | null): string {
    return formatOperationText(value);
  }

  private hasValidPeriod(): boolean {
    return Boolean(this.fromDate && this.toDate && this.fromDate.getTime() <= this.toDate.getTime());
  }

  private buildCriteria(): UsedMaterialSearchCriteria {
    return {
      from: formatLocalDateForApi(this.fromDate) ?? '',
      to: formatLocalDateForApi(this.toDate) ?? '',
      material: this.material,
      product: this.product,
      exitCustomsDocument: this.exitCustomsDocument,
      exitCustomsCode: this.exitCustomsCode,
      page: this.currentPage,
      pageSize: this.pageSize,
    };
  }

  private executeSearch(criteria: UsedMaterialSearchCriteria): Observable<UsedMaterialSearchResult> {
    const requestId = ++this.requestSequence;
    this.isLoading.set(true);
    this.error.set(null);
    this.items.set([]);

    return this.searchUsedMaterials.execute(criteria).pipe(
      map((page) => ({ requestId, page })),
      catchError((error: unknown) => {
        if (requestId === this.requestSequence) {
          this.isLoading.set(false);
          this.error.set(userFacingApiError(error, 'Verifica tu conexión e inténtalo nuevamente.'));
          this.notifications.error('No fue posible consultar los materiales utilizados.');
        }
        return EMPTY;
      }),
    );
  }

  private invalidateResults(): void {
    this.requestSequence += 1;
    this.items.set([]);
    this.totalItems.set(0);
    this.error.set(null);
    this.hasSearched.set(false);
    this.isLoading.set(false);
  }
}
