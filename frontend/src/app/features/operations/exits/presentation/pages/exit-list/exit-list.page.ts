import { Component, DestroyRef, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, Subject, catchError, debounce, filter, map, switchMap, timer } from 'rxjs';
import { userFacingApiError } from '@core/http/api-error.util';
import { NotificationService } from '@core/notifications/notification.service';
import { AppAlertComponent } from '@core/ui/app-alert/app-alert.component';
import { OperationSearchCriteria } from '@features/operations/shared/operation-search-criteria';
import { formatLocalDateForApi, formatOperationDate, formatOperationQuantity, formatOperationText } from '@features/operations/shared/operation-formatters';
import {
  OperationPeriod,
  OperationPeriodFilterComponent,
} from '@features/operations/shared/presentation/operation-period-filter/operation-period-filter.component';
import { ExitLine } from '@features/operations/exits/domain/models/exit-line.model';
import { ExitPage } from '@features/operations/exits/domain/repositories/exit.repository';
import { SearchExitsUseCase } from '@features/operations/exits/application/use-cases/search-exits.use-case';

interface ExitSearchResult {
  requestId: number;
  page: ExitPage;
}

/** Consulta de líneas de exportación por rango de fecha de pago. */
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
  selector: 'app-exit-list',
  template: `
    <div class="min-h-full bg-[#f4f7fb] text-slate-800">
      <main class="mx-auto w-full max-w-360 px-5 py-8 sm:px-8">
        <header class="mb-6">
          <p class="mb-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Operaciones</p>
          <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Salidas</h1>
          <p class="mt-1 text-sm text-slate-500">Consulta de líneas de exportación por rango de fecha de pago.</p>
        </header>

        <section class="mb-4 rounded-2xl border border-slate-200/80 bg-white p-4 shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-labelledby="exit-filters">
          <h2 id="exit-filters" class="sr-only">Filtros de salidas</h2>
          <div class="flex flex-wrap items-center gap-3">
            <app-operation-period-filter #periodFilter class="min-w-0 flex-1" (periodChange)="onPeriodChange($event)" />
            <div class="flex shrink-0 items-center gap-1.5">
              <button
                mat-stroked-button
                type="button"
                class="h-9 rounded-lg! px-3!"
                [attr.aria-expanded]="optionalFiltersExpanded()"
                aria-controls="exit-optional-filters"
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
            <div id="exit-optional-filters" class="mt-3 grid grid-cols-1 gap-3 border-t border-slate-100 pt-3 md:grid-cols-2 xl:grid-cols-4">
              <label>
                <span class="mb-1 block text-xs font-medium text-slate-700">Pedimento</span>
                <input id="exit-customs-document" matInput name="customsDocument" [(ngModel)]="customsDocument" (ngModelChange)="onOptionalFilterChange()" maxlength="60" placeholder="Número de pedimento" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
              </label>
              <label>
                <span class="mb-1 block text-xs font-medium text-slate-700">Clave de pedimento</span>
                <input id="exit-customs-code" matInput name="customsCode" [(ngModel)]="customsCode" (ngModelChange)="onOptionalFilterChange()" maxlength="5" placeholder="Clave" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
              </label>
              <label>
                <span class="mb-1 block text-xs font-medium text-slate-700">Fracción</span>
                <input id="exit-tariff-fraction" matInput name="tariffFraction" [(ngModel)]="tariffFraction" (ngModelChange)="onOptionalFilterChange()" maxlength="12" placeholder="Fracción arancelaria" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
              </label>
              <label>
                <span class="mb-1 block text-xs font-medium text-slate-700">N° parte</span>
                <input id="exit-part-number" matInput name="partNumber" [(ngModel)]="partNumber" (ngModelChange)="onOptionalFilterChange()" maxlength="50" placeholder="Número de parte" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
              </label>
            </div>
          }
        </section>

        @if (hasSearched()) {
          <div class="mb-3 flex min-h-9 items-center justify-between gap-3 px-1">
            <span class="text-xs font-medium text-slate-500" aria-live="polite">{{ formatTotal() }} resultados</span>
            <button mat-button type="button" class="h-9 rounded-lg! px-3! text-slate-600!" (click)="refresh()" [disabled]="!canRefresh()" aria-label="Actualizar consulta de salidas">
              <mat-icon class="text-[18px]!" aria-hidden="true">refresh</mat-icon> Actualizar
            </button>
          </div>
        }

        @if (isLoading()) {
          <div class="space-y-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm" aria-label="Cargando salidas" aria-busy="true">
            @for (row of loadingRows; track row) { <div class="h-10 animate-pulse rounded-lg bg-slate-100"></div> }
          </div>
        } @else if (error()) {
          <section class="rounded-2xl border border-slate-200/80 bg-white p-4 shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-label="Error de consulta">
            <app-alert kind="error" title="No pudimos cargar las salidas" [message]="error()!" actionLabel="Reintentar" (action)="refresh()" />
          </section>
        } @else if (!hasSearched()) {
          <section class="flex min-h-52 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-10 text-center" aria-label="Estado inicial de salidas">
            <mat-icon class="mb-3 h-10 w-10 text-[40px]! text-slate-300" aria-hidden="true">calendar_month</mat-icon>
            <p class="m-0 text-sm font-medium text-slate-600">Selecciona un periodo</p>
            <span class="mt-1 text-xs text-slate-400">para consultar movimientos</span>
          </section>
        } @else {
          <section class="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-label="Resultados de salidas">
            @if (items().length > 0) {
              <div class="overflow-x-auto">
                <table mat-table [dataSource]="items()" class="w-full min-w-210" aria-label="Líneas de salidas">
                  <ng-container matColumnDef="customsDocument">
                    <th mat-header-cell *matHeaderCellDef>Pedimento</th>
                    <td mat-cell *matCellDef="let line" class="font-medium text-slate-700">{{ formatText(line.customsDocument) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="customsCode">
                    <th mat-header-cell *matHeaderCellDef>Clave pedimento</th>
                    <td mat-cell *matCellDef="let line">{{ formatText(line.customsCode) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="paymentDate">
                    <th mat-header-cell *matHeaderCellDef>Fecha de pago</th>
                    <td mat-cell *matCellDef="let line">{{ formatDate(line.paymentDate) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="tariffFraction">
                    <th mat-header-cell *matHeaderCellDef>Fracción</th>
                    <td mat-cell *matCellDef="let line">{{ formatText(line.tariffFraction) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="commercialUnit">
                    <th mat-header-cell *matHeaderCellDef>UMC</th>
                    <td mat-cell *matCellDef="let line">{{ formatText(line.commercialUnit) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="quantity">
                    <th mat-header-cell *matHeaderCellDef>Cantidad</th>
                    <td mat-cell *matCellDef="let line" class="text-right tabular-nums">{{ formatQuantity(line.quantity) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="partNumber">
                    <th mat-header-cell *matHeaderCellDef>N° parte</th>
                    <td mat-cell *matCellDef="let line">{{ formatText(line.partNumber) }}</td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
                </table>
              </div>
            } @else {
              <div class="flex min-h-52 flex-col items-center justify-center px-6 py-10 text-center" aria-label="Sin resultados">
                <mat-icon class="mb-3 h-10 w-10 text-[40px]! text-slate-300" aria-hidden="true">search_off</mat-icon>
                <p class="m-0 text-sm font-medium text-slate-600">No se encontraron movimientos</p>
                <span class="mt-1 text-xs text-slate-400">No hay registros para el periodo y filtros seleccionados.</span>
              </div>
            }
            <mat-paginator [length]="totalItems()" [pageSize]="pageSize" [pageSizeOptions]="pageSizeOptions" (page)="changePage($event)" showFirstLastButtons aria-label="Paginación de salidas" />
          </section>
        }
      </main>
    </div>
  `,
})
export class ExitListPage {
  @ViewChild(OperationPeriodFilterComponent) private periodFilter?: OperationPeriodFilterComponent;

  protected readonly displayedColumns = ['customsDocument', 'customsCode', 'paymentDate', 'tariffFraction', 'commercialUnit', 'quantity', 'partNumber'];
  protected readonly pageSizeOptions = [20, 50, 100];
  protected readonly loadingRows = [1, 2, 3, 4, 5];
  protected readonly items = signal<ExitLine[]>([]);
  protected readonly totalItems = signal(0);
  protected readonly isLoading = signal(false);
  protected readonly hasSearched = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly optionalFiltersExpanded = signal(false);

  protected fromDate: Date | null = null;
  protected toDate: Date | null = null;
  protected customsDocument = '';
  protected customsCode = '';
  protected tariffFraction = '';
  protected partNumber = '';
  protected currentPage = 1;
  protected pageSize = 20;

  private requestSequence = 0;
  private readonly searchTriggers = new Subject<boolean>();
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchExits = inject(SearchExitsUseCase);
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
    return [this.customsDocument, this.customsCode, this.tariffFraction, this.partNumber].filter((value) => value.trim()).length;
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
    this.customsDocument = '';
    this.customsCode = '';
    this.tariffFraction = '';
    this.partNumber = '';
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

  private buildCriteria(): OperationSearchCriteria {
    return {
      from: formatLocalDateForApi(this.fromDate) ?? '',
      to: formatLocalDateForApi(this.toDate) ?? '',
      customsDocument: this.customsDocument,
      customsCode: this.customsCode,
      tariffFraction: this.tariffFraction,
      partNumber: this.partNumber,
      page: this.currentPage,
      pageSize: this.pageSize,
    };
  }

  private executeSearch(criteria: OperationSearchCriteria): Observable<ExitSearchResult> {
    const requestId = ++this.requestSequence;
    this.isLoading.set(true);
    this.error.set(null);
    this.items.set([]);

    return this.searchExits.execute(criteria).pipe(
      map((page) => ({ requestId, page })),
      catchError((error: unknown) => {
        if (requestId === this.requestSequence) {
          this.isLoading.set(false);
          this.error.set(userFacingApiError(error, 'Verifica tu conexión e inténtalo nuevamente.'));
          this.notifications.error('No fue posible consultar las salidas.');
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
