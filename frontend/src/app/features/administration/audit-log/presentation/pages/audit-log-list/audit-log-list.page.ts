import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { EMPTY, Observable, Subject, catchError, debounce, filter, map, switchMap, timer } from 'rxjs';
import { userFacingApiError } from '@core/http/api-error.util';
import { NotificationService } from '@core/notifications/notification.service';
import { AppAlertComponent } from '@core/ui/app-alert/app-alert.component';
import {
  AuditLogEntry,
  AuditLogModule,
  AuditLogPage,
  AuditLogResult,
  AuditLogSearchCriteria,
} from '../../../domain/models/audit-log.model';
import { SearchAuditLogUseCase } from '../../../application/use-cases/search-audit-log.use-case';

/** Combina fecha local y hora local en un instante absoluto. */
export function buildInstant(date: Date | null, time: string | null, endOfSecond = false): Date | null {
  if (!date || Number.isNaN(date.getTime()) || !time) return null;
  const parts = time.split(':');
  const hours = Number(parts[0]);
  const minutes = Number(parts[1]);
  const seconds = parts.length > 2 ? Number(parts[2]) : 0;
  if (!Number.isInteger(hours) || hours < 0 || hours > 23) return null;
  if (!Number.isInteger(minutes) || minutes < 0 || minutes > 59) return null;
  if (!Number.isInteger(seconds) || seconds < 0 || seconds > 59) return null;
  return new Date(date.getFullYear(), date.getMonth(), date.getDate(), hours, minutes, seconds, endOfSecond ? 999 : 0);
}

/** Convierte un instante a ISO-8601 absoluto. */
export function toIsoInstant(value: Date): string {
  return value.toISOString();
}

/** Formatea un instante UTC como fecha/hora local con segundos. */
export function formatAuditLogDate(value: string | null): string {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('es-MX', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  }).format(date);
}

const ACTION_LABELS: Record<string, string> = {
  LOGIN_OK: 'Inicio de sesión',
  LOGIN_FALLIDO: 'Inicio de sesión fallido',
};

/** Etiqueta legible de una acción; tolera acciones futuras desconocidas. */
export function formatAuditLogAction(action: string | null): string {
  if (!action) return '—';
  const known = ACTION_LABELS[action];
  if (known) return known;
  const readable = action
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
  return readable || action;
}

const MODULE_LABELS: Record<AuditLogModule, string> = {
  SEGURIDAD: 'Seguridad',
  CATALOGOS: 'Catálogos',
  OPERACIONES: 'Operaciones',
  ADMINISTRACION: 'Administración',
  FACTURACION: 'Facturación',
  REPORTES: 'Reportes',
  SISTEMA: 'Sistema',
};

const RESULT_LABELS: Record<AuditLogResult, string> = {
  EXITO: 'Éxito',
  FALLO: 'Fallo',
};

function pad2(value: number): string {
  return String(value).padStart(2, '0');
}

function startOfDay(value: Date): Date {
  return new Date(value.getFullYear(), value.getMonth(), value.getDate());
}

function timePart(value: Date): string {
  return `${pad2(value.getHours())}:${pad2(value.getMinutes())}:${pad2(value.getSeconds())}`;
}

interface AuditLogSearchResult {
  requestId: number;
  page: AuditLogPage;
}

/** Consulta read-only paginada de eventos de Bitácora. */
@Component({
  imports: [
    AppAlertComponent,
    FormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatTableModule,
  ],
  providers: [provideNativeDateAdapter(), { provide: MAT_DATE_LOCALE, useValue: 'es-MX' }],
  selector: 'app-audit-log-list',
  template: `
    <div class="min-h-full bg-[#f4f7fb] text-slate-800">
      <main class="mx-auto w-full max-w-360 px-5 py-8 sm:px-8">
        <header class="mb-6">
          <p class="mb-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Administración</p>
          <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Bitácora</h1>
          <p class="mt-1 text-sm text-slate-500">Consulta de eventos de seguridad de la aplicación.</p>
        </header>

        <section class="mb-4 rounded-2xl border border-slate-200/80 bg-white p-4 shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-labelledby="audit-log-filters">
          <h2 id="audit-log-filters" class="sr-only">Filtros de bitácora</h2>

          <div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>Fecha desde</mat-label>
              <input matInput [matDatepicker]="fromPicker" [(ngModel)]="fromDate" (dateChange)="onPeriodChange()" aria-label="Fecha desde" />
              <mat-datepicker-toggle matIconSuffix [for]="fromPicker" aria-hidden="true"></mat-datepicker-toggle>
              <mat-datepicker #fromPicker></mat-datepicker>
            </mat-form-field>
            <label>
              <span class="mb-1 block text-xs font-medium text-slate-700">Hora desde</span>
              <input type="time" step="1" name="fromTime" [(ngModel)]="fromTime" (change)="onPeriodChange()" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
            </label>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>Fecha hasta</mat-label>
              <input matInput [matDatepicker]="toPicker" [(ngModel)]="toDate" (dateChange)="onPeriodChange()" aria-label="Fecha hasta" />
              <mat-datepicker-toggle matIconSuffix [for]="toPicker" aria-hidden="true"></mat-datepicker-toggle>
              <mat-datepicker #toPicker></mat-datepicker>
            </mat-form-field>
            <label>
              <span class="mb-1 block text-xs font-medium text-slate-700">Hora hasta</span>
              <input type="time" step="1" name="toTime" [(ngModel)]="toTime" (change)="onPeriodChange()" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
            </label>
          </div>

          <div class="mt-3 flex flex-wrap items-center justify-between gap-2 border-t border-slate-100 pt-3">
            <div class="flex flex-wrap items-center gap-1.5" aria-label="Periodos rápidos">
              <button type="button" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500/30" (click)="applyPreset('today')">Hoy</button>
              <button type="button" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500/30" (click)="applyPreset('last24h')">Últimas 24 h</button>
              <button type="button" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500/30" (click)="applyPreset('last7d')">Últimos 7 días</button>
            </div>
            <button mat-button type="button" class="h-9 rounded-lg! px-3! text-slate-500!" (click)="clearFilters()">Limpiar</button>
          </div>

          @if (validationMessage()) {
            <p class="mt-2 mb-0 text-xs text-amber-700" aria-live="polite">{{ validationMessage() }}</p>
          }

          <div class="mt-3 grid grid-cols-1 gap-3 border-t border-slate-100 pt-3 md:grid-cols-2 xl:grid-cols-4">
            <label>
              <span class="mb-1 block text-xs font-medium text-slate-700">Usuario ID</span>
              <input id="audit-log-user-id" matInput type="number" min="1" name="userIdFilter" [(ngModel)]="userIdFilter" (ngModelChange)="onDebouncedFilterChange()" placeholder="Ej. 1" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
            </label>
            <label>
              <span class="mb-1 block text-xs font-medium text-slate-700">Módulo</span>
              <select id="audit-log-module" name="moduleFilter" [(ngModel)]="moduleFilter" (ngModelChange)="onImmediateFilterChange()" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10">
                <option value="">Todos</option>
                <option value="SEGURIDAD">Seguridad</option>
                <option value="CATALOGOS">Catálogos</option>
                <option value="OPERACIONES">Operaciones</option>
                <option value="ADMINISTRACION">Administración</option>
                <option value="FACTURACION">Facturación</option>
                <option value="REPORTES">Reportes</option>
                <option value="SISTEMA">Sistema</option>
              </select>
            </label>
            <label>
              <span class="mb-1 block text-xs font-medium text-slate-700">Resultado</span>
              <select id="audit-log-result" name="resultFilter" [(ngModel)]="resultFilter" (ngModelChange)="onImmediateFilterChange()" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10">
                <option value="">Todos</option>
                <option value="EXITO">Éxito</option>
                <option value="FALLO">Fallo</option>
              </select>
            </label>
            <label>
              <span class="mb-1 block text-xs font-medium text-slate-700">Correlation ID</span>
              <input id="audit-log-correlation-id" matInput type="text" name="correlationFilter" [(ngModel)]="correlationFilter" (ngModelChange)="onDebouncedFilterChange()" maxlength="40" placeholder="Correlation ID exacto" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
            </label>
          </div>
        </section>

        @if (hasSearched() && !validationMessage()) {
          <div class="mb-3 flex min-h-9 items-center justify-between gap-3 px-1">
            <span class="text-xs font-medium text-slate-500" aria-live="polite">{{ formatTotal() }} resultados</span>
            <button mat-button type="button" class="h-9 rounded-lg! px-3! text-slate-600!" (click)="refresh()" [disabled]="!canRefresh()" aria-label="Actualizar consulta de bitácora">
              <mat-icon class="text-[18px]!" aria-hidden="true">refresh</mat-icon> Actualizar
            </button>
          </div>
        }

        @if (validationMessage()) {
          <section class="flex min-h-40 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-8 text-center" aria-label="Filtros incompletos">
            <mat-icon class="mb-2 h-8 w-8 text-[32px]! text-slate-300" aria-hidden="true">tune</mat-icon>
            <p class="m-0 text-sm font-medium text-slate-600">Corrige los filtros para consultar la bitácora.</p>
          </section>
        } @else if (isLoading()) {
          <div class="space-y-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm" aria-label="Cargando eventos de bitácora" aria-busy="true">
            @for (row of loadingRows; track row) { <div class="h-10 animate-pulse rounded-lg bg-slate-100"></div> }
          </div>
        } @else if (error()) {
          <section class="rounded-2xl border border-slate-200/80 bg-white p-4 shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-label="Error de consulta">
            <app-alert kind="error" title="No pudimos cargar la bitácora" [message]="error()!" actionLabel="Reintentar" (action)="refresh()" />
          </section>
        } @else {
          <section class="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-label="Resultados de bitácora">
            @if (items().length > 0) {
              <div class="overflow-x-auto">
                <table mat-table [dataSource]="items()" class="w-full min-w-240" aria-label="Eventos de bitácora">
                  <ng-container matColumnDef="fecha">
                    <th mat-header-cell *matHeaderCellDef>Fecha</th>
                    <td mat-cell *matCellDef="let row" [attr.title]="row.date">{{ formatDate(row.date) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="usuario">
                    <th mat-header-cell *matHeaderCellDef>Usuario</th>
                    <td mat-cell *matCellDef="let row" class="font-medium text-slate-700">{{ formatUser(row) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="modulo">
                    <th mat-header-cell *matHeaderCellDef>Módulo</th>
                    <td mat-cell *matCellDef="let row">{{ formatModule(row.module) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="accion">
                    <th mat-header-cell *matHeaderCellDef>Acción</th>
                    <td mat-cell *matCellDef="let row">{{ formatAction(row.action) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="resultado">
                    <th mat-header-cell *matHeaderCellDef>Resultado</th>
                    <td mat-cell *matCellDef="let row">
                      <span
                        class="inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] font-semibold"
                        [class.bg-emerald-50]="row.result === 'EXITO'"
                        [class.text-emerald-700]="row.result === 'EXITO'"
                        [class.border-emerald-200]="row.result === 'EXITO'"
                        [class.bg-rose-50]="row.result === 'FALLO'"
                        [class.text-rose-700]="row.result === 'FALLO'"
                        [class.border-rose-200]="row.result === 'FALLO'"
                      >{{ formatResult(row.result) }}</span>
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="detalle">
                    <th mat-header-cell *matHeaderCellDef>Detalle</th>
                    <td mat-cell *matCellDef="let row" class="max-w-60 truncate text-slate-500" [attr.title]="row.detail ?? ''">{{ formatDetail(row.detail) }}</td>
                  </ng-container>
                  <ng-container matColumnDef="correlationId">
                    <th mat-header-cell *matHeaderCellDef>Correlation ID</th>
                    <td mat-cell *matCellDef="let row" class="font-mono text-xs text-slate-500">{{ formatDetail(row.correlationId) }}</td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
                </table>
              </div>
            } @else {
              <div class="flex min-h-52 flex-col items-center justify-center px-6 py-10 text-center" aria-label="Sin resultados">
                <mat-icon class="mb-3 h-10 w-10 text-[40px]! text-slate-300" aria-hidden="true">search_off</mat-icon>
                <p class="m-0 text-sm font-medium text-slate-600">No se encontraron eventos</p>
                <span class="mt-1 text-xs text-slate-400">No hay eventos para el periodo y filtros seleccionados.</span>
              </div>
            }
            <mat-paginator [length]="totalItems()" [pageIndex]="currentPage - 1" [pageSize]="pageSize" [pageSizeOptions]="pageSizeOptions" (page)="changePage($event)" showFirstLastButtons aria-label="Paginación de bitácora" />
          </section>
        }
      </main>
    </div>
  `,
})
export class AuditLogListPage {
  protected readonly displayedColumns = ['fecha', 'usuario', 'modulo', 'accion', 'resultado', 'detalle', 'correlationId'];
  protected readonly pageSizeOptions = [20, 50, 100];
  protected readonly loadingRows = [1, 2, 3, 4, 5];
  protected readonly items = signal<AuditLogEntry[]>([]);
  protected readonly totalItems = signal(0);
  protected readonly isLoading = signal(false);
  protected readonly hasSearched = signal(false);
  protected readonly error = signal<string | null>(null);

  protected fromDate: Date | null = null;
  protected fromTime = '00:00:00';
  protected toDate: Date | null = null;
  protected toTime = '00:00:00';
  protected userIdFilter: number | null = null;
  protected moduleFilter: AuditLogModule | '' = '';
  protected resultFilter: AuditLogResult | '' = '';
  protected correlationFilter = '';
  protected currentPage = 1;
  protected pageSize = 20;

  private requestSequence = 0;
  private readonly searchTriggers = new Subject<boolean>();
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchAuditLog = inject(SearchAuditLogUseCase);
  private readonly notifications = inject(NotificationService);

  constructor() {
    this.searchTriggers
      .pipe(
        debounce((immediate) => timer(immediate ? 0 : 500)),
        map(() => this.buildCriteria()),
        filter((criteria): criteria is AuditLogSearchCriteria => criteria !== null),
        switchMap((criteria) => this.executeSearch(criteria)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ requestId, page }) => {
        if (requestId !== this.requestSequence) return;
        this.items.set(page.items);
        this.totalItems.set(page.total);
        this.isLoading.set(false);
      });

    this.applyPreset('today');
    this.hasSearched.set(true);
    this.scheduleSearch(true);
  }

  protected validationMessage(): string | null {
    if (!this.fromDate || !this.fromTime || !this.toDate || !this.toTime) {
      return 'Selecciona fecha y hora de inicio y fin.';
    }
    const from = buildInstant(this.fromDate, this.fromTime);
    const to = buildInstant(this.toDate, this.toTime);
    if (!from || !to) return 'El rango de fechas no es válido.';
    if (from.getTime() > to.getTime()) {
      return 'La fecha y hora de inicio no pueden ser posteriores a las finales.';
    }
    if (this.userIdFilter !== null && this.userIdFilter < 1) {
      return 'El ID de usuario debe ser mayor o igual que 1.';
    }
    return null;
  }

  protected applyPreset(preset: 'today' | 'last24h' | 'last7d'): void {
    const now = new Date();
    let from = startOfDay(now);

    if (preset === 'last24h') {
      from = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    } else if (preset === 'last7d') {
      from = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
    }

    this.setRange(from, now);
    this.currentPage = 1;
    this.hasSearched.set(true);
    this.scheduleSearch(true);
  }

  protected onPeriodChange(): void {
    this.currentPage = 1;
    if (this.validationMessage() !== null) {
      this.invalidateResults();
      return;
    }
    this.hasSearched.set(true);
    this.scheduleSearch(true);
  }

  protected onImmediateFilterChange(): void {
    if (this.validationMessage() !== null) {
      this.invalidateResults();
      return;
    }
    this.currentPage = 1;
    this.hasSearched.set(true);
    this.scheduleSearch(true);
  }

  protected onDebouncedFilterChange(): void {
    if (this.validationMessage() !== null) {
      this.invalidateResults();
      return;
    }
    this.currentPage = 1;
    this.hasSearched.set(true);
    this.scheduleSearch(false);
  }

  protected refresh(): void {
    if (!this.canRefresh()) return;
    this.scheduleSearch(true);
  }

  protected canRefresh(): boolean {
    return this.hasSearched() && this.validationMessage() === null;
  }

  protected clearFilters(): void {
    this.userIdFilter = null;
    this.moduleFilter = '';
    this.resultFilter = '';
    this.correlationFilter = '';
    this.setRange(startOfDay(new Date()), new Date());
    this.currentPage = 1;
    this.invalidateResults();
    this.hasSearched.set(true);
    this.scheduleSearch(true);
  }

  protected changePage(event: PageEvent): void {
    if (!this.canRefresh()) return;
    this.currentPage = event.pageIndex + 1;
    this.pageSize = event.pageSize;
    this.scheduleSearch(true);
  }

  protected formatTotal(): string {
    return new Intl.NumberFormat('es-MX').format(this.totalItems());
  }

  protected formatDate(value: string): string {
    return formatAuditLogDate(value);
  }

  protected formatUser(entry: AuditLogEntry): string {
    if (entry.user && entry.user.trim()) return entry.user;
    if (entry.userId !== null) return `ID ${entry.userId}`;
    return 'Sistema / anónimo';
  }

  protected formatModule(module: AuditLogModule): string {
    return MODULE_LABELS[module] ?? module;
  }

  protected formatAction(action: string): string {
    return formatAuditLogAction(action);
  }

  protected formatResult(result: AuditLogResult): string {
    return RESULT_LABELS[result] ?? result;
  }

  protected formatDetail(value: string | null): string {
    return value?.trim() ? value : '—';
  }

  private setRange(from: Date, to: Date): void {
    this.fromDate = startOfDay(from);
    this.fromTime = timePart(from);
    this.toDate = startOfDay(to);
    this.toTime = timePart(to);
  }

  private buildCriteria(): AuditLogSearchCriteria | null {
    if (this.validationMessage() !== null) return null;
    const from = buildInstant(this.fromDate, this.fromTime);
    const to = buildInstant(this.toDate, this.toTime, true);
    if (!from || !to) return null;

    return {
      from: toIsoInstant(from),
      to: toIsoInstant(to),
      userId: this.userIdFilter,
      module: this.moduleFilter || null,
      result: this.resultFilter || null,
      correlationId: this.correlationFilter.trim(),
      page: this.currentPage,
      pageSize: this.pageSize,
    };
  }

  private invalidateResults(): void {
    this.requestSequence += 1;
    this.items.set([]);
    this.totalItems.set(0);
    this.error.set(null);
    this.isLoading.set(false);
    this.hasSearched.set(false);
  }

  private scheduleSearch(immediate: boolean): void {
    this.requestSequence += 1;
    this.searchTriggers.next(immediate);
  }

  private executeSearch(criteria: AuditLogSearchCriteria): Observable<AuditLogSearchResult> {
    const requestId = ++this.requestSequence;
    this.isLoading.set(true);
    this.error.set(null);
    this.items.set([]);

    return this.searchAuditLog.execute(criteria).pipe(
      map((page) => ({ requestId, page })),
      catchError((error: unknown) => {
        if (requestId === this.requestSequence) {
          this.isLoading.set(false);
          this.error.set(userFacingApiError(error, 'Verifica tu conexión e inténtalo nuevamente.'));
          this.notifications.error('No fue posible consultar la bitácora.');
        }
        return EMPTY;
      }),
    );
  }
}
