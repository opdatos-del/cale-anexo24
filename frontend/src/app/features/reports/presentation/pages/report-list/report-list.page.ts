import { Component, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { EMPTY, catchError, finalize } from 'rxjs';
import { AuthService } from '@core/auth/auth.service';
import { userFacingApiError } from '@core/http/api-error.util';
import { NotificationService } from '@core/notifications/notification.service';
import { AppAlertComponent } from '@core/ui/app-alert/app-alert.component';
import { formatLocalDateForApi, formatOperationDate, formatOperationQuantity, formatOperationText } from '@features/operations/shared/operation-formatters';
import { OperationPeriod, OperationPeriodFilterComponent } from '@features/operations/shared/presentation/operation-period-filter/operation-period-filter.component';
import { ExportReportUseCase } from '../../../application/use-cases/export-report.use-case';
import { SearchReportUseCase } from '../../../application/use-cases/search-report.use-case';
import { ReportRow, ReportSearchCriteria, ReportType } from '../../../domain/models/report.model';

interface ReportOption {
  type: ReportType | 'saldos';
  label: string;
  icon: string;
  available: boolean;
}

interface ReportColumn {
  key: string;
  label: string;
  format?: 'date' | 'quantity';
}

const REPORTS: ReportOption[] = [
  { type: 'entradas', label: 'Entradas', icon: 'move_to_inbox', available: true },
  { type: 'salidas', label: 'Salidas', icon: 'outbox', available: true },
  { type: 'materiales-utilizados', label: 'Materiales utilizados', icon: 'layers', available: true },
  { type: 'bitacora', label: 'Bitácora', icon: 'manage_search', available: true },
  { type: 'saldos', label: 'Saldos', icon: 'account_balance_wallet', available: false },
];

const COLUMNS: Record<ReportType, ReportColumn[]> = {
  entradas: [
    { key: 'pedimento', label: 'Pedimento' }, { key: 'clavePedimento', label: 'Clave' },
    { key: 'fechaEntrada', label: 'Fecha de entrada', format: 'date' }, { key: 'fechaPago', label: 'Fecha de pago', format: 'date' },
    { key: 'fraccion', label: 'Fracción' }, { key: 'unidadComercial', label: 'UMC' },
    { key: 'cantidadComercial', label: 'Cantidad', format: 'quantity' }, { key: 'numeroParte', label: 'N° parte' },
  ],
  salidas: [
    { key: 'pedimento', label: 'Pedimento' }, { key: 'clavePedimento', label: 'Clave' },
    { key: 'fechaPago', label: 'Fecha de pago', format: 'date' }, { key: 'fraccion', label: 'Fracción' },
    { key: 'unidadComercial', label: 'UMC' }, { key: 'cantidad', label: 'Cantidad', format: 'quantity' }, { key: 'numeroParte', label: 'N° parte' },
  ],
  'materiales-utilizados': [
    { key: 'fecha', label: 'Fecha', format: 'date' }, { key: 'pedimentoEntrada', label: 'Pedimento entrada' },
    { key: 'pedimentoSalida', label: 'Pedimento salida' }, { key: 'materialCode', label: 'Material' },
    { key: 'materialDescription', label: 'Descripción material' }, { key: 'productCode', label: 'Producto' },
    { key: 'productDescription', label: 'Descripción producto' }, { key: 'cantidadIncorporada', label: 'Incorporada', format: 'quantity' },
    { key: 'cantidadMerma', label: 'Merma', format: 'quantity' }, { key: 'cantidadDesperdicio', label: 'Desperdicio', format: 'quantity' },
    { key: 'cantidadTotalDescargada', label: 'Total descargado', format: 'quantity' }, { key: 'unidad', label: 'Unidad' },
  ],
  bitacora: [
    { key: 'fecha', label: 'Fecha', format: 'date' }, { key: 'usuario', label: 'Usuario' },
    { key: 'modulo', label: 'Módulo' }, { key: 'accion', label: 'Acción' }, { key: 'resultado', label: 'Resultado' },
    { key: 'detalle', label: 'Detalle' }, { key: 'correlationId', label: 'Correlation ID' },
  ],
};

/** Genera reportes V1 paginados para el periodo obligatorio seleccionado. */
@Component({
  imports: [AppAlertComponent, FormsModule, MatButtonModule, MatIconModule, MatInputModule, MatPaginatorModule, MatTableModule, OperationPeriodFilterComponent],
  selector: 'app-report-list',
  template: `
    <div class="min-h-full bg-[#f4f7fb] text-slate-800">
      <main class="mx-auto w-full max-w-360 px-5 py-8 sm:px-8">
        <header class="mb-6">
          <p class="mb-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Consultas consolidadas</p>
          <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Reportes</h1>
          <p class="mt-1 text-sm text-slate-500">Selecciona un reporte, define el periodo obligatorio y genera la consulta.</p>
        </header>

        <section class="mb-4 rounded-2xl border border-slate-200/80 bg-white p-4 shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-label="Configuración del reporte">
          <div class="grid grid-cols-1 gap-2 sm:grid-cols-2 xl:grid-cols-5" role="list" aria-label="Tipo de reporte">
            @for (report of reports; track report.type) {
              <button type="button" class="flex min-h-20 items-center gap-3 rounded-xl border px-3 text-left transition disabled:cursor-not-allowed disabled:opacity-55" [class.border-blue-500]="selectedType() === report.type" [class.bg-blue-50]="selectedType() === report.type" [class.border-slate-200]="selectedType() !== report.type" [class.bg-white]="selectedType() !== report.type" [disabled]="!report.available" [attr.aria-current]="selectedType() === report.type ? 'true' : null" (click)="selectReport(report)">
                <mat-icon [class.text-blue-600]="selectedType() === report.type" [class.text-slate-400]="selectedType() !== report.type" aria-hidden="true">{{ report.icon }}</mat-icon>
                <span class="text-sm font-medium text-slate-700">{{ report.label }} @if (!report.available) { <small class="block text-xs font-normal text-slate-400">No disponible</small> }</span>
              </button>
            }
          </div>

          <div class="mt-4 border-t border-slate-100 pt-4">
            <app-operation-period-filter #periodFilter (periodChange)="onPeriodChange($event)" />
            @if (periodMessage()) { <p class="mb-0 mt-2 text-xs text-amber-700" aria-live="polite">{{ periodMessage() }}</p> }
          </div>

          <div class="mt-4 border-t border-slate-100 pt-4">
            @if (isOperationalReport()) {
              <div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
                <label><span class="mb-1 block text-xs font-medium text-slate-700">Pedimento</span><input matInput name="customsDocument" [(ngModel)]="customsDocument" maxlength="50" class="report-input" /></label>
                <label><span class="mb-1 block text-xs font-medium text-slate-700">Clave de pedimento</span><input matInput name="customsCode" [(ngModel)]="customsCode" maxlength="5" class="report-input" /></label>
                @if (selectedType() !== 'materiales-utilizados') {
                  <label><span class="mb-1 block text-xs font-medium text-slate-700">Fracción</span><input matInput name="tariffFraction" [(ngModel)]="tariffFraction" maxlength="15" class="report-input" /></label>
                  <label><span class="mb-1 block text-xs font-medium text-slate-700">N° parte</span><input matInput name="partNumber" [(ngModel)]="partNumber" maxlength="50" class="report-input" /></label>
                } @else {
                  <label><span class="mb-1 block text-xs font-medium text-slate-700">Material</span><input matInput name="material" [(ngModel)]="material" maxlength="50" class="report-input" /></label>
                  <label><span class="mb-1 block text-xs font-medium text-slate-700">Producto</span><input matInput name="product" [(ngModel)]="product" maxlength="50" class="report-input" /></label>
                }
              </div>
            } @else {
              <div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
                <label><span class="mb-1 block text-xs font-medium text-slate-700">Usuario ID</span><input matInput type="number" min="1" name="userId" [(ngModel)]="userId" class="report-input" /></label>
                <label><span class="mb-1 block text-xs font-medium text-slate-700">Módulo</span><input matInput name="module" [(ngModel)]="module" maxlength="30" class="report-input" /></label>
                <label><span class="mb-1 block text-xs font-medium text-slate-700">Resultado</span><input matInput name="result" [(ngModel)]="result" maxlength="20" class="report-input" /></label>
                <label><span class="mb-1 block text-xs font-medium text-slate-700">Correlation ID</span><input matInput name="correlationId" [(ngModel)]="correlationId" maxlength="40" class="report-input" /></label>
              </div>
            }
          </div>

          <div class="mt-4 flex flex-wrap justify-end gap-2 border-t border-slate-100 pt-4">
            <button mat-button type="button" (click)="clearFilters()">Limpiar</button>
            <button mat-flat-button type="button" [disabled]="!canGenerate() || isLoading()" (click)="generate()"><mat-icon aria-hidden="true">assessment</mat-icon>Generar</button>
          </div>
        </section>

        @if (hasGenerated()) {
          <div class="mb-3 flex min-h-9 items-center justify-between gap-3 px-1">
            <span class="text-xs font-medium text-slate-500" aria-live="polite">{{ formatTotal() }} resultados</span>
            <div class="flex gap-1">
              @if (canExport()) { <button mat-button type="button" [disabled]="isExporting()" (click)="exportXlsx()"><mat-icon aria-hidden="true">download</mat-icon>XLSX</button> }
              <button mat-button type="button" [disabled]="isLoading()" (click)="generate()"><mat-icon aria-hidden="true">refresh</mat-icon>Actualizar</button>
            </div>
          </div>
        }

        @if (isLoading()) {
          <div class="space-y-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm" aria-label="Generando reporte" aria-busy="true">@for (row of loadingRows; track row) { <div class="h-10 animate-pulse rounded-lg bg-slate-100"></div> }</div>
        } @else if (error()) {
          <section class="rounded-2xl border border-slate-200/80 bg-white p-4 shadow-sm"><app-alert kind="error" title="No pudimos generar el reporte" [message]="error()!" actionLabel="Reintentar" (action)="generate()" /></section>
        } @else if (!hasGenerated()) {
          <section class="flex min-h-52 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-10 text-center"><mat-icon class="mb-3 h-10 w-10 text-[40px]! text-slate-300" aria-hidden="true">assessment</mat-icon><p class="m-0 text-sm font-medium text-slate-600">Configura y genera un reporte</p><span class="mt-1 text-xs text-slate-400">El periodo inicial y final son obligatorios.</span></section>
        } @else {
          <section class="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-sm" aria-label="Resultados del reporte">
            @if (items().length) {
              <div class="overflow-x-auto"><table mat-table [dataSource]="items()" class="w-full min-w-240">@for (column of columns(); track column.key) { <ng-container [matColumnDef]="column.key"><th mat-header-cell *matHeaderCellDef>{{ column.label }}</th><td mat-cell *matCellDef="let row" [class.text-right]="column.format === 'quantity'" [class.tabular-nums]="column.format === 'quantity'">{{ formatCell(row, column) }}</td></ng-container> }<tr mat-header-row *matHeaderRowDef="displayedColumns()"></tr><tr mat-row *matRowDef="let row; columns: displayedColumns()"></tr></table></div>
            } @else {
              <div class="flex min-h-52 flex-col items-center justify-center px-6 py-10 text-center"><mat-icon class="mb-3 h-10 w-10 text-[40px]! text-slate-300" aria-hidden="true">search_off</mat-icon><p class="m-0 text-sm font-medium text-slate-600">No se encontraron resultados</p><span class="mt-1 text-xs text-slate-400">No hay registros para el periodo y filtros seleccionados.</span></div>
            }
            <mat-paginator [length]="totalItems()" [pageIndex]="currentPage - 1" [pageSize]="pageSize" [pageSizeOptions]="pageSizeOptions" (page)="changePage($event)" showFirstLastButtons aria-label="Paginación del reporte" />
          </section>
        }
      </main>
    </div>
  `,
  styles: [`
    .report-input { width: 100%; height: 2.5rem; border: 1px solid rgb(226 232 240); border-radius: .5rem; background: rgb(248 250 252); padding: 0 .75rem; font-size: .875rem; outline: none; }
    .report-input:focus { border-color: rgb(59 130 246); background: white; box-shadow: 0 0 0 4px rgb(59 130 246 / 10%); }
  `],
})
export class ReportListPage {
  @ViewChild(OperationPeriodFilterComponent) private periodFilter?: OperationPeriodFilterComponent;

  protected readonly reports = REPORTS;
  protected readonly pageSizeOptions = [20, 50, 100];
  protected readonly loadingRows = [1, 2, 3, 4, 5];
  protected readonly selectedType = signal<ReportType>('entradas');
  protected readonly items = signal<ReportRow[]>([]);
  protected readonly totalItems = signal(0);
  protected readonly isLoading = signal(false);
  protected readonly isExporting = signal(false);
  protected readonly hasGenerated = signal(false);
  protected readonly error = signal<string | null>(null);
  protected fromDate: Date | null = null;
  protected toDate: Date | null = null;
  protected customsDocument = '';
  protected customsCode = '';
  protected tariffFraction = '';
  protected partNumber = '';
  protected material = '';
  protected product = '';
  protected userId: number | null = null;
  protected module = '';
  protected result = '';
  protected correlationId = '';
  protected currentPage = 1;
  protected pageSize = 20;

  private readonly auth = inject(AuthService);
  private readonly searchReport = inject(SearchReportUseCase);
  private readonly exportReport = inject(ExportReportUseCase);
  private readonly notifications = inject(NotificationService);

  protected selectReport(report: ReportOption): void {
    if (!report.available || report.type === 'saldos') return;
    this.selectedType.set(report.type);
    this.resetResults();
  }

  protected onPeriodChange(period: OperationPeriod): void {
    this.fromDate = period.start;
    this.toDate = period.end;
    this.resetResults();
  }

  protected isOperationalReport(): boolean { return this.selectedType() !== 'bitacora'; }
  protected periodMessage(): string | null {
    if (!this.fromDate || !this.toDate) return 'Selecciona fecha inicial y fecha final para generar el reporte.';
    return this.fromDate.getTime() > this.toDate.getTime() ? 'La fecha inicial no puede ser posterior a la final.' : null;
  }
  protected canGenerate(): boolean { return this.periodMessage() === null && (this.userId === null || this.userId >= 1); }
  protected columns(): ReportColumn[] { return COLUMNS[this.selectedType()]; }
  protected displayedColumns(): string[] { return this.columns().map((column) => column.key); }
  protected formatTotal(): string { return new Intl.NumberFormat('es-MX').format(this.totalItems()); }
  protected canExport(): boolean { return this.auth.hasPermission('REPORTES_EXPORTAR') && this.hasGenerated() && this.items().length > 0; }

  protected generate(): void {
    if (!this.canGenerate()) return;
    this.isLoading.set(true);
    this.error.set(null);
    this.items.set([]);
    this.searchReport.execute(this.criteria()).pipe(
      catchError((error: unknown) => { this.error.set(userFacingApiError(error, 'Verifica tu conexión e inténtalo nuevamente.')); this.notifications.error('No fue posible generar el reporte.'); return EMPTY; }),
      finalize(() => this.isLoading.set(false)),
    ).subscribe((page) => { this.items.set(page.items); this.totalItems.set(page.total); this.hasGenerated.set(true); });
  }

  protected exportXlsx(): void {
    if (!this.canExport()) return;
    this.isExporting.set(true);
    this.exportReport.execute(this.criteria()).pipe(finalize(() => this.isExporting.set(false))).subscribe({
      next: (file) => this.download(file),
      error: (error: unknown) => { this.notifications.error(userFacingApiError(error, 'No fue posible exportar el reporte.')); },
    });
  }

  protected clearFilters(): void {
    this.periodFilter?.clear(); this.fromDate = null; this.toDate = null; this.customsDocument = ''; this.customsCode = ''; this.tariffFraction = ''; this.partNumber = ''; this.material = ''; this.product = ''; this.userId = null; this.module = ''; this.result = ''; this.correlationId = ''; this.currentPage = 1; this.resetResults();
  }

  protected changePage(event: PageEvent): void { if (!this.hasGenerated()) return; this.currentPage = event.pageIndex + 1; this.pageSize = event.pageSize; this.generate(); }
  protected formatCell(row: ReportRow, column: ReportColumn): string {
    const value = row[column.key] ?? null;
    if (column.format === 'date') return formatOperationDate(typeof value === 'string' ? value : null);
    if (column.format === 'quantity') return formatOperationQuantity(value);
    return formatOperationText(typeof value === 'string' ? value : value?.toString() ?? null);
  }

  private criteria(): ReportSearchCriteria {
    return { type: this.selectedType(), from: formatLocalDateForApi(this.fromDate) ?? '', to: formatLocalDateForApi(this.toDate) ?? '', page: this.currentPage, pageSize: this.pageSize, customsDocument: this.customsDocument, customsCode: this.customsCode, tariffFraction: this.tariffFraction, partNumber: this.partNumber, material: this.material, product: this.product, userId: this.userId, module: this.module, result: this.result, correlationId: this.correlationId };
  }
  private resetResults(): void { this.items.set([]); this.totalItems.set(0); this.hasGenerated.set(false); this.error.set(null); this.isLoading.set(false); }
  private download(file: Blob): void { const url = URL.createObjectURL(file); const link = document.createElement('a'); link.href = url; link.download = `${this.selectedType()}.xlsx`; link.click(); URL.revokeObjectURL(url); }
}
