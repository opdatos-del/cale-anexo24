import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CatalogImportResponse, CatalogImportType } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { UploadCatalogImportUseCase } from '@features/catalogs/imports/application/use-cases/upload-catalog-import.use-case';
import { NotificationService } from '@core/notifications/notification.service';
import { AuthService } from '@core/auth/auth.service';

@Component({
  selector: 'app-catalog-import',
  imports: [MatButtonModule, MatIconModule],
  template: `
    <main class="min-h-full bg-slate-50 px-4 py-7 text-slate-800 sm:px-7 lg:px-9">
      <header class="mx-auto mb-6 w-full max-w-7xl">
        <p class="mb-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Catálogos · staging V1</p>
        <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Importar materiales y productos</h1>
        <p class="mt-2 text-sm text-slate-500">Validación y previsualización aisladas. No modifica CALE_IMMEX ni confirma catálogos.</p>
      </header>

      <section class="mx-auto w-full max-w-7xl rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6" aria-label="Contrato de importación">
        <div class="mb-5 flex flex-wrap gap-2" role="tablist" aria-label="Tipo de catálogo">
          @if (canUploadMaterial()) { <button mat-stroked-button type="button" class="rounded-xl!" [class.border-blue-500]="type() === 'MATERIAL'" [class.bg-blue-50]="type() === 'MATERIAL'" [attr.aria-selected]="type() === 'MATERIAL'" (click)="selectType('MATERIAL')">Materiales</button> }
          @if (canUploadProduct()) { <button mat-stroked-button type="button" class="rounded-xl!" [class.border-blue-500]="type() === 'PRODUCTO'" [class.bg-blue-50]="type() === 'PRODUCTO'" [attr.aria-selected]="type() === 'PRODUCTO'" (click)="selectType('PRODUCTO')">Productos</button> }
        </div>

        <div class="flex flex-col gap-3 rounded-xl border border-dashed border-slate-300 bg-slate-50 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div><h2 class="m-0 text-sm font-semibold text-slate-800">Selecciona un archivo</h2><p class="mb-0 mt-1 text-xs text-slate-500">.xls o .xlsx · máximo 10 MiB · contrato derivado de CargaMaterial/tmpproductos</p></div>
          <label class="inline-flex cursor-pointer items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-700">
            <mat-icon aria-hidden="true">upload_file</mat-icon> Elegir archivo
            <input class="sr-only" type="file" accept=".xls,.xlsx" (change)="onFileSelected($event)" />
          </label>
        </div>
        @if (selectedFile()) { <p class="mt-3 text-sm text-slate-600">Archivo: <strong>{{ selectedFile()!.name }}</strong></p> }
        @if (selectionError()) { <p class="mt-3 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800" role="alert">{{ selectionError() }}</p> }
        @if (isLoading()) { <div class="mt-5 h-24 animate-pulse rounded-xl bg-slate-100" aria-label="Validando archivo" aria-busy="true"></div> }
        @if (error()) { <div class="mt-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert"><p class="m-0 font-semibold">No pudimos validar la carga</p><p class="mb-3 mt-1">{{ error() }}</p><button mat-stroked-button type="button" (click)="retry()">Reintentar</button></div> }

        @if (result(); as current) {
          <div class="mt-5 grid gap-3 sm:grid-cols-4" aria-label="Totales de carga">
            <div class="rounded-xl bg-slate-50 p-3"><span class="block text-xs text-slate-500">Estado</span><strong>{{ current.estado }}</strong></div>
            <div class="rounded-xl bg-slate-50 p-3"><span class="block text-xs text-slate-500">Filas</span><strong>{{ current.totalFilas }}</strong></div>
            <div class="rounded-xl bg-slate-50 p-3"><span class="block text-xs text-slate-500">Válidas</span><strong>{{ current.filasValidas }}</strong></div>
            <div class="rounded-xl bg-slate-50 p-3"><span class="block text-xs text-slate-500">Inválidas</span><strong>{{ current.filasInvalidas }}</strong></div>
          </div>
          @if (current.errores.length) { <section class="mt-5 rounded-xl border border-red-200 bg-red-50 p-4" aria-label="Errores"><h2 class="m-0 text-sm font-semibold text-red-900">Errores de validación</h2>@for (issue of current.errores; track $index) { <p class="mb-0 mt-2 text-sm text-red-800">{{ issue.fila ? 'Fila ' + issue.fila + ' · ' : '' }}{{ issue.columna || 'Estructura' }} · {{ issue.mensaje }}</p> }</section> }
          @if (current.filas.length) { <div class="mt-5 overflow-x-auto rounded-xl border border-slate-200"><table class="w-full min-w-max text-left text-xs"><thead class="bg-slate-50"><tr>@for (column of current.columnas; track column) { <th class="whitespace-nowrap px-3 py-3 font-semibold text-slate-600">{{ column }}</th> }</tr></thead><tbody class="divide-y divide-slate-100">@for (row of current.filas; track $index) { <tr>@for (column of current.columnas; track column) { <td class="whitespace-nowrap px-3 py-3 text-slate-600">{{ row[column] || '—' }}</td> }</tr> }</tbody></table></div> }
          <p class="mt-5 rounded-xl border border-blue-100 bg-blue-50 p-4 text-sm text-blue-900">Previsualización validada. La confirmación hacia CALE_IMMEX todavía no está habilitada.</p>
        }
      </section>
    </main>
  `,
})
export class CatalogImportPage {
  private readonly auth = inject(AuthService);
  protected readonly canUploadMaterial = computed(() => this.auth.hasPermission('MATERIALES_CARGAR'));
  protected readonly canUploadProduct = computed(() => this.auth.hasPermission('PRODUCTOS_CARGAR'));
  protected readonly type = signal<CatalogImportType>(this.auth.hasPermission('MATERIALES_CARGAR') ? 'MATERIAL' : 'PRODUCTO');
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly selectionError = signal<string | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly result = signal<CatalogImportResponse | null>(null);
  private readonly upload = inject(UploadCatalogImportUseCase);
  private readonly notifications = inject(NotificationService);

  selectType(type: CatalogImportType): void {
    this.type.set(type); this.selectedFile.set(null); this.selectionError.set(null); this.error.set(null); this.result.set(null);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectionError.set(null); this.error.set(null); this.result.set(null);
    if (!file) return;
    if (file.size > 10 * 1024 * 1024 || !/\.(xls|xlsx)$/i.test(file.name)) {
      this.selectedFile.set(null); this.selectionError.set('Selecciona un archivo .xls o .xlsx de máximo 10 MiB.'); return;
    }
    this.selectedFile.set(file); this.submit(file);
  }

  retry(): void { const file = this.selectedFile(); if (file) this.submit(file); }

  private submit(file: File): void {
    this.isLoading.set(true); this.error.set(null);
    this.upload.execute(this.type(), file).subscribe({
      next: response => { this.result.set(response); this.isLoading.set(false); },
      error: err => { this.error.set(err?.error?.message || 'Revisa el archivo y vuelve a intentar.'); this.isLoading.set(false); this.notifications.error('No fue posible validar el archivo.'); },
    });
  }
}
