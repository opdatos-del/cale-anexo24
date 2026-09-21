import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { userFacingApiError } from '@core/http/api-error.util';
import { NotificationService } from '@core/notifications/notification.service';
import { AppAlertComponent } from '@core/ui/app-alert/app-alert.component';
import {
  formatOperationDate,
  formatOperationQuantity,
  formatOperationText,
} from '@features/operations/shared/operation-formatters';
import { ExitLine } from '../../../domain/models/exit-line.model';
import { SearchExitsUseCase } from '../../../application/use-cases/search-exits.use-case';

/** Consulta de líneas de exportación por rango de fecha de pago. */
@Component({
  imports: [AppAlertComponent, FormsModule, MatButtonModule, MatIconModule, MatInputModule, MatPaginatorModule, MatTableModule],
  selector: 'app-exit-list',
  template: `
    <div class="min-h-full bg-[#f4f7fb] text-slate-800">
      <main class="mx-auto w-full max-w-360 px-5 py-8 sm:px-8">
        <header class="mb-7">
          <p class="mb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Operaciones</p>
          <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Salidas</h1>
          <p class="mt-2 text-sm text-slate-500">Consulta de líneas de exportación por rango de fecha de pago.</p>
        </header>

        <section class="mb-6 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-labelledby="exit-filters">
          <h2 id="exit-filters" class="sr-only">Filtros de salidas</h2>
          <form class="grid grid-cols-1 items-end gap-4 md:grid-cols-2 xl:grid-cols-6" (ngSubmit)="search()">
            <label>
              <span class="mb-1 block text-xs font-medium text-slate-700">Desde <span class="text-blue-600" aria-hidden="true">*</span></span>
              <input id="exit-from" matInput type="date" name="from" [(ngModel)]="from" (input)="updateRangeError()" class="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" aria-required="true" />
            </label>
            <label>
              <span class="mb-1 block text-xs font-medium text-slate-700">Hasta <span class="text-blue-600" aria-hidden="true">*</span></span>
              <input id="exit-to" matInput type="date" name="to" [(ngModel)]="to" (input)="updateRangeError()" class="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" aria-required="true" />
            </label>
            <label>
              <span class="mb-1 block text-xs font-medium text-slate-700">Pedimento</span>
              <input id="exit-customs-document" matInput name="customsDocument" [(ngModel)]="customsDocument" maxlength="60" placeholder="Número de pedimento" class="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
            </label>
            <label>
              <span class="mb-1 block text-xs font-medium text-slate-700">Clave de pedimento</span>
              <input id="exit-customs-code" matInput name="customsCode" [(ngModel)]="customsCode" maxlength="5" placeholder="Clave" class="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
            </label>
            <label>
              <span class="mb-1 block text-xs font-medium text-slate-700">Fracción</span>
              <input id="exit-tariff-fraction" matInput name="tariffFraction" [(ngModel)]="tariffFraction" maxlength="12" placeholder="Fracción arancelaria" class="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
            </label>
            <label>
              <span class="mb-1 block text-xs font-medium text-slate-700">N° parte</span>
              <input id="exit-part-number" matInput name="partNumber" [(ngModel)]="partNumber" maxlength="50" placeholder="Número de parte" class="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
            </label>
            <div class="flex flex-wrap gap-3 md:col-span-2 xl:col-span-6">
              <button mat-flat-button color="primary" type="submit" class="h-11 min-w-28 rounded-xl!" [disabled]="!canSearch()">Consultar</button>
              <button mat-stroked-button type="button" class="h-11 min-w-24 rounded-xl!" (click)="clearFilters()">Limpiar</button>
            </div>
          </form>
          @if (rangeError()) {
            <div class="mt-4">
              <app-alert kind="warning" [message]="rangeError()!" />
            </div>
          }
        </section>

        <div class="mb-4 flex flex-wrap items-center gap-3">
          <button mat-stroked-button type="button" class="h-10 rounded-xl!" (click)="refresh()" [disabled]="!canRefresh()" aria-label="Actualizar consulta de salidas">
            <mat-icon>refresh</mat-icon> Actualizar
          </button>
          <span class="text-xs text-slate-500" aria-live="polite">{{ totalItems() }} registros encontrados</span>
        </div>

        @if (isLoading()) {
          <div class="space-y-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm" aria-label="Cargando salidas" aria-busy="true">
            @for (row of loadingRows; track row) { <div class="h-10 animate-pulse rounded-lg bg-slate-100"></div> }
          </div>
        } @else if (error()) {
          <app-alert kind="error" title="No pudimos cargar las salidas" [message]="error()!" actionLabel="Reintentar" (action)="refresh()" />
        } @else if (!hasSearched()) {
          <app-alert kind="info" title="Consulta de salidas" message="Define un rango de fechas para consultar movimientos." />
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
              <div class="p-4">
                <app-alert kind="info" title="Sin resultados" message="No se encontraron movimientos con los filtros aplicados." />
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
  protected readonly displayedColumns = ['customsDocument', 'customsCode', 'paymentDate', 'tariffFraction', 'commercialUnit', 'quantity', 'partNumber'];
  protected readonly pageSizeOptions = [20, 50, 100];
  protected readonly loadingRows = [1, 2, 3, 4, 5];
  protected readonly items = signal<ExitLine[]>([]);
  protected readonly totalItems = signal(0);
  protected readonly isLoading = signal(false);
  protected readonly hasSearched = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly rangeError = signal<string | null>(null);

  protected from = '';
  protected to = '';
  protected customsDocument = '';
  protected customsCode = '';
  protected tariffFraction = '';
  protected partNumber = '';
  protected currentPage = 1;
  protected pageSize = 20;

  private requestSequence = 0;
  private readonly searchExits = inject(SearchExitsUseCase);
  private readonly notifications = inject(NotificationService);

  protected canSearch(): boolean {
    return this.validateRange() === null;
  }

  protected canRefresh(): boolean {
    return this.hasSearched() && this.canSearch();
  }

  protected search(): void {
    const validation = this.validateRange();
    if (validation) {
      this.rangeError.set(validation);
      return;
    }

    this.rangeError.set(null);
    this.currentPage = 1;
    this.hasSearched.set(true);
    this.loadExits();
  }

  protected refresh(): void {
    if (this.canRefresh()) this.loadExits();
  }

  protected clearFilters(): void {
    this.requestSequence += 1;
    this.from = '';
    this.to = '';
    this.customsDocument = '';
    this.customsCode = '';
    this.tariffFraction = '';
    this.partNumber = '';
    this.currentPage = 1;
    this.items.set([]);
    this.totalItems.set(0);
    this.error.set(null);
    this.rangeError.set(null);
    this.hasSearched.set(false);
    this.isLoading.set(false);
  }

  protected updateRangeError(): void {
    this.rangeError.set(this.from || this.to ? this.validateRange() : null);
  }

  protected changePage(event: PageEvent): void {
    this.currentPage = event.pageIndex + 1;
    this.pageSize = event.pageSize;
    this.loadExits();
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

  private validateRange(): string | null {
    if (!this.from || !this.to) return 'Defina el rango de fechas.';
    if (this.from > this.to) return 'La fecha inicial no puede ser posterior a la final.';
    return null;
  }

  private loadExits(): void {
    if (!this.canSearch()) return;

    const requestId = ++this.requestSequence;
    this.isLoading.set(true);
    this.error.set(null);
    this.items.set([]);

    this.searchExits
      .execute({
        from: this.from,
        to: this.to,
        customsDocument: this.customsDocument,
        customsCode: this.customsCode,
        tariffFraction: this.tariffFraction,
        partNumber: this.partNumber,
        page: this.currentPage,
        pageSize: this.pageSize,
      })
      .subscribe({
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
          this.notifications.error('No fue posible consultar las salidas.');
        },
      });
  }
}
