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
  FixedAsset,
  FixedAssetPage,
  FixedAssetSearchCriteria,
} from '@features/operations/fixed-assets/domain/models/fixed-asset.model';
import { SearchFixedAssetsUseCase } from '@features/operations/fixed-assets/application/use-cases/search-fixed-assets.use-case';

interface FixedAssetSearchResult {
  requestId: number;
  page: FixedAssetPage;
}

/** Consulta paginada de partidas de importación marcadas como activos fijos. */
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
  selector: 'app-fixed-asset-list',
  template: `
    <div class="min-h-full bg-[#f4f7fb] text-slate-800">
      <main class="mx-auto w-full max-w-360 px-5 py-8 sm:px-8">
        <header class="mb-6">
          <p class="mb-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Operaciones</p>
          <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Activos fijos</h1>
          <p class="mt-1 text-sm text-slate-500">Consulta de partidas de importación marcadas como activos fijos.</p>
        </header>

        <section class="mb-4 rounded-2xl border border-slate-200/80 bg-white p-4 shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-labelledby="fixed-asset-filters">
          <h2 id="fixed-asset-filters" class="sr-only">Filtros de activos fijos</h2>
          <div class="flex flex-wrap items-center gap-3">
            <app-operation-period-filter #periodFilter class="min-w-0 flex-1" (periodChange)="onPeriodChange($event)" />
            <div class="flex shrink-0 items-center gap-1.5">
              <button
                mat-stroked-button
                type="button"
                class="h-9 rounded-lg! px-3!"
                [attr.aria-expanded]="optionalFiltersExpanded()"
                aria-controls="fixed-asset-optional-filters"
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

          @if (hasInvalidDateFilter()) {
            <p class="mt-2 mb-0 text-xs text-amber-700" aria-live="polite">Selecciona ambas fechas para filtrar por periodo.</p>
          }

          @if (optionalFiltersExpanded()) {
            <div id="fixed-asset-optional-filters" class="mt-3 grid grid-cols-1 gap-3 border-t border-slate-100 pt-3 md:grid-cols-2 xl:grid-cols-4">
              <label>
                <span class="mb-1 block text-xs font-medium text-slate-700">Pedimento</span>
                <input id="fixed-asset-customs-document" matInput name="customsDocument" [(ngModel)]="customsDocument" (ngModelChange)="onOptionalFilterChange()" maxlength="20" placeholder="Documento de importación" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
              </label>
              <label>
                <span class="mb-1 block text-xs font-medium text-slate-700">Clave de pedimento</span>
                <input id="fixed-asset-customs-code" matInput name="customsCode" [(ngModel)]="customsCode" (ngModelChange)="onOptionalFilterChange()" maxlength="5" placeholder="Clave" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
              </label>
              <label>
                <span class="mb-1 block text-xs font-medium text-slate-700">Número de parte</span>
                <input id="fixed-asset-part-number" matInput name="partNumber" [(ngModel)]="partNumber" (ngModelChange)="onOptionalFilterChange()" maxlength="50" placeholder="Número de parte" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
              </label>
              <label>
                <span class="mb-1 block text-xs font-medium text-slate-700">Descripción</span>
                <input id="fixed-asset-description" matInput name="description" [(ngModel)]="description" (ngModelChange)="onOptionalFilterChange()" maxlength="250" placeholder="Descripción" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
              </label>
              <label>
                <span class="mb-1 block text-xs font-medium text-slate-700">Serie</span>
                <input id="fixed-asset-serial-number" matInput name="serialNumber" [(ngModel)]="serialNumber" (ngModelChange)="onOptionalFilterChange()" maxlength="50" placeholder="Número de serie" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
              </label>
              <label>
                <span class="mb-1 block text-xs font-medium text-slate-700">Marca</span>
                <input id="fixed-asset-brand" matInput name="brand" [(ngModel)]="brand" (ngModelChange)="onOptionalFilterChange()" maxlength="50" placeholder="Marca" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
              </label>
              <label>
                <span class="mb-1 block text-xs font-medium text-slate-700">Modelo</span>
                <input id="fixed-asset-model" matInput name="model" [(ngModel)]="model" (ngModelChange)="onOptionalFilterChange()" maxlength="50" placeholder="Modelo" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
              </label>
            </div>
          }
        </section>

        @if (hasSearched()) {
          <div class="mb-3 flex min-h-9 items-center justify-between gap-3 px-1">
            <span class="text-xs font-medium text-slate-500" aria-live="polite">{{ formatTotal() }} resultados</span>
            <button mat-button type="button" class="h-9 rounded-lg! px-3! text-slate-600!" (click)="refresh()" [disabled]="!canRefresh()" aria-label="Actualizar consulta de activos fijos">
              <mat-icon class="text-[18px]!" aria-hidden="true">refresh</mat-icon> Actualizar
            </button>
          </div>
        }

        @if (isLoading()) {
          <div class="space-y-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm" aria-label="Cargando activos fijos" aria-busy="true">
            @for (row of loadingRows; track row) { <div class="h-10 animate-pulse rounded-lg bg-slate-100"></div> }
          </div>
        } @else if (error()) {
          <section class="rounded-2xl border border-slate-200/80 bg-white p-4 shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-label="Error de consulta">
            <app-alert kind="error" title="No pudimos cargar los activos fijos" [message]="error()!" actionLabel="Reintentar" (action)="refresh()" />
          </section>
        } @else if (hasInvalidDateFilter()) {
          <section class="flex min-h-52 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-10 text-center" aria-label="Periodo incompleto">
            <mat-icon class="mb-3 h-10 w-10 text-[40px]! text-slate-300" aria-hidden="true">date_range</mat-icon>
            <p class="m-0 text-sm font-medium text-slate-600">Completa el periodo para aplicar el filtro</p>
            <span class="mt-1 text-xs text-slate-400">También puedes limpiar ambas fechas para consultar todos los activos.</span>
          </section>
        } @else {
          <section class="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-label="Resultados de activos fijos">
            @if (items().length > 0) {
              <div class="overflow-x-auto">
                <table mat-table [dataSource]="items()" class="w-full min-w-275" aria-label="Activos fijos">
                  <ng-container matColumnDef="importDate">
                    <th mat-header-cell *matHeaderCellDef>Fecha importación</th>
                    <td mat-cell *matCellDef="let item">{{ formatDate(item.importDate) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="customsDocument">
                    <th mat-header-cell *matHeaderCellDef>Pedimento</th>
                    <td mat-cell *matCellDef="let item" class="font-medium text-slate-700">{{ formatText(item.customsDocument) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="customsCode">
                    <th mat-header-cell *matHeaderCellDef>Clave</th>
                    <td mat-cell *matCellDef="let item">{{ formatText(item.customsCode) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="part">
                    <th mat-header-cell *matHeaderCellDef>N° parte / Descripción</th>
                    <td mat-cell *matCellDef="let item">
                      <div class="font-medium text-slate-700">{{ formatText(item.partNumber) }}</div>
                      <div class="mt-0.5 text-xs text-slate-400">{{ formatText(item.description) }}</div>
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="tariffFraction">
                    <th mat-header-cell *matHeaderCellDef>Fracción</th>
                    <td mat-cell *matCellDef="let item">{{ formatText(item.tariffFraction) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="quantity">
                    <th mat-header-cell *matHeaderCellDef>Cantidad</th>
                    <td mat-cell *matCellDef="let item" class="text-right tabular-nums">{{ formatQuantity(item.quantity) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="unit">
                    <th mat-header-cell *matHeaderCellDef>Unidad</th>
                    <td mat-cell *matCellDef="let item">{{ formatText(item.unit) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="serialNumber">
                    <th mat-header-cell *matHeaderCellDef>Serie</th>
                    <td mat-cell *matCellDef="let item">{{ formatText(item.serialNumber) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="brand">
                    <th mat-header-cell *matHeaderCellDef>Marca</th>
                    <td mat-cell *matCellDef="let item">{{ formatText(item.brand) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="model">
                    <th mat-header-cell *matHeaderCellDef>Modelo</th>
                    <td mat-cell *matCellDef="let item">{{ formatText(item.model) }}</td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
                </table>
              </div>
            } @else {
              <div class="flex min-h-52 flex-col items-center justify-center px-6 py-10 text-center" aria-label="Sin resultados">
                <mat-icon class="mb-3 h-10 w-10 text-[40px]! text-slate-300" aria-hidden="true">search_off</mat-icon>
                <p class="m-0 text-sm font-medium text-slate-600">No se encontraron activos fijos</p>
                <span class="mt-1 text-xs text-slate-400">No hay registros para los filtros seleccionados.</span>
              </div>
            }
            <mat-paginator [length]="totalItems()" [pageIndex]="currentPage - 1" [pageSize]="pageSize" [pageSizeOptions]="pageSizeOptions" (page)="changePage($event)" showFirstLastButtons aria-label="Paginación de activos fijos" />
          </section>
        }
      </main>
    </div>
  `,
})
export class FixedAssetListPage {
  @ViewChild(OperationPeriodFilterComponent) private periodFilter?: OperationPeriodFilterComponent;

  protected readonly displayedColumns = [
    'importDate',
    'customsDocument',
    'customsCode',
    'part',
    'tariffFraction',
    'quantity',
    'unit',
    'serialNumber',
    'brand',
    'model',
  ];
  protected readonly pageSizeOptions = [20, 50, 100];
  protected readonly loadingRows = [1, 2, 3, 4, 5];
  protected readonly items = signal<FixedAsset[]>([]);
  protected readonly totalItems = signal(0);
  protected readonly isLoading = signal(false);
  protected readonly hasSearched = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly optionalFiltersExpanded = signal(false);

  protected fromDate: Date | null = null;
  protected toDate: Date | null = null;
  protected customsDocument = '';
  protected customsCode = '';
  protected partNumber = '';
  protected description = '';
  protected serialNumber = '';
  protected brand = '';
  protected model = '';
  protected currentPage = 1;
  protected pageSize = 20;

  private requestSequence = 0;
  private readonly searchTriggers = new Subject<boolean>();
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchFixedAssets = inject(SearchFixedAssetsUseCase);
  private readonly notifications = inject(NotificationService);

  constructor() {
    this.searchTriggers
      .pipe(
        debounce((immediate) => timer(immediate ? 0 : 500)),
        map(() => this.buildCriteria()),
        filter(() => this.hasValidDateFilter()),
        switchMap((criteria) => this.executeSearch(criteria)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ requestId, page }) => {
        if (requestId !== this.requestSequence) return;
        this.items.set(page.items);
        this.totalItems.set(page.total);
        this.isLoading.set(false);
      });

    this.hasSearched.set(true);
    this.scheduleSearch(true);
  }

  protected onPeriodChange(period: OperationPeriod): void {
    this.fromDate = period.start;
    this.toDate = period.end;

    if (!this.hasValidDateFilter()) {
      this.invalidateResults();
      return;
    }

    this.currentPage = 1;
    this.hasSearched.set(true);
    this.scheduleSearch(true);
  }

  protected onOptionalFilterChange(): void {
    this.optionalFiltersExpanded.set(true);
    if (!this.hasValidDateFilter()) return;

    this.currentPage = 1;
    this.hasSearched.set(true);
    this.scheduleSearch(false);
  }

  protected toggleOptionalFilters(): void {
    this.optionalFiltersExpanded.update((expanded) => !expanded);
  }

  protected activeFilterCount(): number {
    return [
      this.customsDocument,
      this.customsCode,
      this.partNumber,
      this.description,
      this.serialNumber,
      this.brand,
      this.model,
    ].filter((value) => value.trim()).length;
  }

  protected formatTotal(): string {
    return new Intl.NumberFormat('es-MX').format(this.totalItems());
  }

  protected canRefresh(): boolean {
    return this.hasSearched() && this.hasValidDateFilter();
  }

  protected refresh(): void {
    if (!this.canRefresh()) return;
    this.scheduleSearch(true);
  }

  protected clearFilters(): void {
    this.requestSequence += 1;
    this.periodFilter?.clear();
    this.fromDate = null;
    this.toDate = null;
    this.customsDocument = '';
    this.customsCode = '';
    this.partNumber = '';
    this.description = '';
    this.serialNumber = '';
    this.brand = '';
    this.model = '';
    this.currentPage = 1;
    this.items.set([]);
    this.totalItems.set(0);
    this.error.set(null);
    this.isLoading.set(false);
    this.optionalFiltersExpanded.set(false);
    this.hasSearched.set(true);
    this.scheduleSearch(true);
  }

  protected changePage(event: PageEvent): void {
    if (!this.hasValidDateFilter()) return;
    this.currentPage = event.pageIndex + 1;
    this.pageSize = event.pageSize;
    this.scheduleSearch(true);
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

  protected hasInvalidDateFilter(): boolean {
    return !this.hasValidDateFilter();
  }

  private hasValidDateFilter(): boolean {
    const hasFrom = this.fromDate !== null;
    const hasTo = this.toDate !== null;

    if (!hasFrom && !hasTo) return true;
    return Boolean(this.fromDate && this.toDate && this.fromDate.getTime() <= this.toDate.getTime());
  }

  private buildCriteria(): FixedAssetSearchCriteria {
    return {
      from: formatLocalDateForApi(this.fromDate),
      to: formatLocalDateForApi(this.toDate),
      customsDocument: this.customsDocument,
      customsCode: this.customsCode,
      partNumber: this.partNumber,
      description: this.description,
      serialNumber: this.serialNumber,
      brand: this.brand,
      model: this.model,
      page: this.currentPage,
      pageSize: this.pageSize,
    };
  }

  private scheduleSearch(immediate: boolean): void {
    this.requestSequence += 1;
    this.searchTriggers.next(immediate);
  }

  private executeSearch(criteria: FixedAssetSearchCriteria): Observable<FixedAssetSearchResult> {
    const requestId = ++this.requestSequence;
    this.isLoading.set(true);
    this.error.set(null);
    this.items.set([]);

    return this.searchFixedAssets.execute(criteria).pipe(
      map((page) => ({ requestId, page })),
      catchError((error: unknown) => {
        if (requestId === this.requestSequence) {
          this.isLoading.set(false);
          this.error.set(userFacingApiError(error, 'Verifica tu conexión e inténtalo nuevamente.'));
          this.notifications.error('No fue posible consultar los activos fijos.');
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
