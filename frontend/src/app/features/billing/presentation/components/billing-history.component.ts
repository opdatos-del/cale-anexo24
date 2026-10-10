import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { userFacingApiError } from '@core/http/api-error.util';
import {
  BillingLoadDetail,
  BillingLoadSummary,
  BillingPage,
  BillingPersistedLoadStatus,
  BillingValidationError,
} from '@features/billing/domain/models/billing-upload.model';
import { BillingApiService } from '@features/billing/infrastructure/api/billing-api.service';

@Component({
  selector: 'app-billing-history',
  imports: [CommonModule, MatPaginatorModule],
  template: ` <section class="space-y-5" aria-label="Historial de cargas de facturación"> <header class="flex items-end justify-between gap-3"> <div> <h2 class="m-0 text-xl font-semibold text-slate-900">Historial de cargas</h2> <p class="mb-0 mt-1 text-sm text-slate-500"> Revisa tus cargas previsualizadas e inválidas. </p> </div> <button type="button" (click)="search()" [disabled]="loading()">Actualizar</button> </header> <div class="grid gap-3 rounded-xl border border-slate-200 bg-white p-4 sm:grid-cols-3" aria-label="Filtros de historial" > <label >Estado <select (change)="setStatus(status.value)" #status> <option value="">Todos</option> <option value="PREVISUALIZADA">Previsualizada</option> <option value="INVALIDA">Inválida</option> </select></label ><label >Desde <input type="date" [value]="desde ?? ''" (change)="setPeriod(from.value, to.value)" #from /></label ><label >Hasta <input type="date" [value]="hasta ?? ''" (change)="setPeriod(from.value, to.value)" #to /></label> </div> @if (loading()) { <p role="status">Cargando historial…</p> } @if (error(); as message) { <p role="alert"> {{ message }} <button type="button" (click)="search()">Reintentar</button> </p> } @if (detailError(); as detailMessage) { <p role="alert">{{ detailMessage }}</p> } @if (detail(); as carga) { <article aria-label="Detalle de carga persistida"> <header> <p>Revisión persistida</p> <h3>{{ carga.archivo }}</h3> <p>Hash: {{ carga.hash }}</p> <button type="button" (click)="closeReview()">Cerrar revisión</button> </header> <div> <span >Estado persistido: <strong>{{ persistedLabel(carga.estado) }}</strong></span ><span >Total: <strong>{{ carga.totalRegistros }}</strong></span ><span >Válidos: <strong>{{ carga.registrosValidos }}</strong></span ><span >Inválidos: <strong>{{ carga.registrosInvalidos }}</strong></span > </div> @if (detailLoading()) { <p role="status">Cargando detalle…</p> } <h4>Vista previa</h4> @if (detailColumns(carga).length) { <div class="overflow-x-auto"> <table> <thead> <tr> @for (column of detailColumns(carga); track column) { <th>{{ column }}</th> } </tr> </thead> <tbody> @for (row of carga.preview.filas; track $index) { <tr> @for (column of detailColumns(carga); track column) { <td>{{ detailValue(row, column) }}</td> } </tr> } </tbody> </table> </div> } @else { <p>No hay filas disponibles para mostrar.</p> } @if (carga.totalRegistros > 0) { <mat-paginator [length]="carga.totalRegistros" [pageIndex]="carga.preview.pagina - 1" [pageSize]="carga.preview.tamano" [pageSizeOptions]="[20, 50, 100]" showFirstLastButtons aria-label="Paginación de vista previa persistida" (page)="changeDetailPage($event)" /> } @if (carga.errores.length) { <section> <h4>Errores de validación</h4> @for (issue of carga.errores; track $index) { <div> <strong>{{ issue.mensaje }}</strong> <p> {{ errorLocation(issue) }} · {{ issue.codigo }} @if (issue.valorEnmascarado) { · Valor: {{ issue.valorEnmascarado }} } </p> </div> } </section> } </article> } @if (page(); as result) { @if (result.items.length === 0) { <p>No hay cargas que coincidan con los filtros.</p> } @for (load of result.items; track load.id) { <article> <p>{{ load.fecha | date: 'dd/MM/yyyy' }}</p> <h3>{{ load.archivo }}</h3> <p> {{ persistedLabel(load.estado) }} · {{ load.totalRegistros }} registros · {{ load.registrosValidos }} válidos · {{ load.registrosInvalidos }} inválidos </p> <button type="button" (click)="review(load.id)" [attr.aria-label]="reviewLabel(load)"> Revisar </button> </article> } <mat-paginator [length]="result.total" [pageIndex]="result.pagina - 1" [pageSize]="result.tamano" [pageSizeOptions]="[20, 50, 100]" showFirstLastButtons aria-label="Paginación del historial" (page)="changePage($event)" /> } </section> `,
})
export class BillingHistoryComponent implements OnInit {
  private readonly api = inject(BillingApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly page = signal(null as BillingPage<BillingLoadSummary> | null);
  protected readonly detail = signal(null as BillingLoadDetail | null);
  protected readonly loading = signal(false);
  protected readonly detailLoading = signal(false);
  protected readonly error = signal(null as string | null);
  protected readonly detailError = signal(null as string | null);
  private status: BillingPersistedLoadStatus | null = null;
  private currentPage = 1;
  private pageSize = 20;
  protected desde: string | null = null;
  protected hasta: string | null = null;

  ngOnInit(): void {
    this.search();
    const id = Number(this.route.snapshot.queryParamMap.get('carga'));
    if (Number.isSafeInteger(id) && id > 0) this.review(id, false);
  }
  protected setStatus(value: string): void {
    this.status = value === 'PREVISUALIZADA' || value === 'INVALIDA' ? value : null;
    this.currentPage = 1;
    this.search();
  }
  protected setPeriod(desde: string, hasta: string): void {
    this.desde = desde || null;
    this.hasta = hasta || null;
    if (
      (this.desde === null) !== (this.hasta === null) ||
      (this.desde !== null && this.desde > this.hasta!)
    )
      return;
    this.currentPage = 1;
    this.search();
  }
  protected setPageSize(value: string): void {
    const size = Number(value);
    if (![20, 50, 100].includes(size)) return;
    this.pageSize = size;
    this.currentPage = 1;
    this.search();
  }
  protected changePage(event: PageEvent): void {
    this.currentPage = event.pageIndex + 1;
    this.pageSize = event.pageSize;
    this.search();
  }
  protected changeDetailPage(event: PageEvent): void {
    const current = this.detail();
    if (current && !this.detailLoading())
      this.loadDetail(current.id, event.pageIndex + 1, event.pageSize);
  }
  protected search(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    this.api
      .history(this.status, this.desde, this.hasta, this.currentPage, this.pageSize)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.page.set(result);
          this.currentPage = result.pagina;
          this.pageSize = result.tamano;
        },
        error: (cause: unknown) =>
          this.error.set(userFacingApiError(cause, 'No fue posible cargar el historial.')),
      });
  }
  protected review(id: number, updateUrl = true): void {
    if (!Number.isSafeInteger(id) || id <= 0 || this.detailLoading()) return;
    if (updateUrl)
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { carga: id },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      });
    this.loadDetail(id, 1, 100);
  }
  private loadDetail(id: number, page: number, size: number): void {
    this.detailLoading.set(true);
    this.detailError.set(null);
    this.api
      .load(id, page, size)
      .pipe(finalize(() => this.detailLoading.set(false)))
      .subscribe({
        next: (detail) => this.detail.set(detail),
        error: (cause: unknown) => {
          this.detail.set(null);
          this.detailError.set(detailAccessError(cause));
        },
      });
  }
  protected closeReview(): void {
    this.detail.set(null);
    this.detailError.set(null);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { carga: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
  protected detailColumns(detail: BillingLoadDetail): string[] {
    return Object.keys(detail.preview.filas[0] ?? {});
  }
  protected detailValue(row: Record<string, string | null>, column: string): string {
    return row[column] ?? '—';
  }
  protected persistedLabel(status: BillingPersistedLoadStatus): string {
    return status === 'PREVISUALIZADA' ? 'Previsualizada' : 'Inválida';
  }
  protected errorLocation(issue: BillingValidationError): string {
    return (
      [issue.hoja, issue.fila === null ? null : 'Fila ' + issue.fila, issue.columna]
        .filter(Boolean)
        .join(' · ') || 'Ubicación no especificada'
    );
  }
  protected reviewLabel(load: BillingLoadSummary): string {
    return 'Revisar ' + load.archivo;
  }
}

/** Conserva mensaje neutral para cargas inexistentes o ajenas. */
function detailAccessError(cause: unknown): string {
  if (cause instanceof HttpErrorResponse && cause.status === 404) {
    const correlationId = typeof cause.error?.correlationId === 'string' ? cause.error.correlationId : null;
    return 'No fue posible encontrar esta carga.' + (correlationId ? ' Referencia: ' + correlationId : '');
  }
  return userFacingApiError(cause, 'No fue posible encontrar esta carga.');
}
