import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BillingLoadDetail, BillingLoadSummary, BillingPage, BillingPersistedLoadStatus } from '@features/billing/domain/models/billing-upload.model';
import { BillingApiService } from '@features/billing/infrastructure/api/billing-api.service';
import { userFacingApiError } from '@core/http/api-error.util';
import { finalize } from 'rxjs';

@Component({ selector: 'app-billing-history', imports: [CommonModule], template: '@if (loading()) { <p role="status">Cargando historial…</p> } @if (error(); as message) { <p role="alert">{{ message }} <button type="button" (click)="search()">Reintentar</button></p> } <h2>Historial</h2><button type="button" (click)="search()">Actualizar</button> @if (detail(); as carga) { <article><button type="button" (click)="closeReview()">Cerrar</button> <strong>{{ carga.archivo }}</strong> {{ persistedLabel(carga.estado) }} · {{ carga.preview.filas.length }} filas · {{ carga.errores.length }} errores</article> } @if (page(); as result) { @for (load of result.items; track load.id) { <article> {{ load.fecha | date:"dd/MM/yyyy" }} · {{ load.archivo }} · {{ persistedLabel(load.estado) }} · {{ load.totalRegistros }} registros <button type="button" (click)="review(load.id)" [attr.aria-label]="reviewLabel(load)">Revisar</button></article> } <button type="button" [disabled]="result.pagina === 1" (click)="goTo(result.pagina - 1)">Anterior</button> {{ result.pagina }} / {{ result.total }} <button type="button" [disabled]="result.pagina * result.tamano >= result.total" (click)="goTo(result.pagina + 1)">Siguiente</button> }' })
export class BillingHistoryComponent implements OnInit {
  private readonly api = inject(BillingApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly page = signal(null as BillingPage<BillingLoadSummary> | null);
  protected readonly detail = signal(null as BillingLoadDetail | null);
  protected readonly loading = signal(false);
  protected readonly detailLoading = signal(false);
  protected readonly error = signal(null as string | null);
  private status: BillingPersistedLoadStatus | null = null;
  private currentPage = 1;
  private pageSize = 20;

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

  protected setPageSize(value: string): void {
    this.pageSize = Number(value);
    this.currentPage = 1;
    this.search();
  }

  protected goTo(page: number): void {
    this.currentPage = page;
    this.search();
  }

  protected search(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    this.api.history(this.status, null, null, this.currentPage, this.pageSize)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({ next: (result) => this.page.set(result), error: (cause: unknown) => this.error.set(userFacingApiError(cause, 'No fue posible cargar el historial.')) });
  }

  protected review(id: number, updateUrl = true): void {
    if (!Number.isSafeInteger(id) || id <= 0 || this.detailLoading()) return;
    if (updateUrl) this.router.navigate([], { relativeTo: this.route, queryParams: { carga: id }, queryParamsHandling: 'merge', replaceUrl: true });
    this.detailLoading.set(true);
    this.api.load(id, 1, 100).pipe(finalize(() => this.detailLoading.set(false))).subscribe({
      next: (detail) => this.detail.set(detail),
      error: () => { this.detail.set(null); this.error.set('No fue posible encontrar esta carga.'); },
    });
  }

  protected closeReview(): void {
    this.detail.set(null);
    this.router.navigate([], { relativeTo: this.route, queryParams: { carga: null }, queryParamsHandling: 'merge', replaceUrl: true });
  }

  protected persistedLabel(status: BillingPersistedLoadStatus): string {
    return status === 'PREVISUALIZADA' ? 'Previsualizada' : 'Inválida';
  }

  protected reviewLabel(load: BillingLoadSummary): string { return 'Revisar ' + load.archivo; }
}
