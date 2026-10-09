import { Component, computed, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { CatalogImportResponse, CatalogImportType, CatalogMaterialImportConfirmation, CatalogProductImportConfirmation, CatalogClientImportConfirmation, CatalogProviderImportConfirmation, CatalogAgentImportConfirmation } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { ConfirmCatalogMaterialImportUseCase } from '@features/catalogs/imports/application/use-cases/confirm-catalog-material-import.use-case';
import { ConfirmCatalogProductImportUseCase } from '@features/catalogs/imports/application/use-cases/confirm-catalog-product-import.use-case';
import { ConfirmCatalogClientImportUseCase } from '@features/catalogs/imports/application/use-cases/confirm-catalog-client-import.use-case';
import { ConfirmCatalogProviderImportUseCase } from '@features/catalogs/imports/application/use-cases/confirm-catalog-provider-import.use-case';
import { ConfirmCatalogAgentImportUseCase } from '@features/catalogs/imports/application/use-cases/confirm-catalog-agent-import.use-case';
import { UploadCatalogImportUseCase } from '@features/catalogs/imports/application/use-cases/upload-catalog-import.use-case';
import { GetCatalogImportPreviewUseCase } from '@features/catalogs/imports/application/use-cases/get-catalog-import-preview.use-case';
import { CatalogImportErrorCsvService } from '@features/catalogs/imports/application/catalog-import-error-csv.service';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notifications/notification.service';
import { ConfirmService } from '@core/ui/confirm-dialog/confirm.service';

@Component({
  selector: 'app-catalog-import',
  imports: [MatButtonModule, MatIconModule, MatPaginatorModule],
  template: `
    <main class="min-h-full bg-slate-50 px-4 py-7 text-slate-800 sm:px-7 lg:px-9">
      <header class="mx-auto mb-6 w-full max-w-7xl">
        <p class="mb-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Catálogos · staging V1</p>
        <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Importar materiales, productos, clientes, proveedores y agentes aduanales</h1>
        <p class="mt-2 text-sm text-slate-500">Valida y previsualiza archivos. Las importaciones de catálogos con permiso de confirmar pueden aplicar las reglas vigentes del sistema Anexo 24.</p>
      </header>

      <section class="mx-auto w-full max-w-7xl rounded-2xl border border-slate-200 bg-white p-4 sm:p-6" aria-label="Contrato de importación">
        <div class="mb-5 flex flex-wrap gap-2" role="tablist" aria-label="Tipo de catálogo">
          @if (canUploadMaterial()) { <button mat-stroked-button type="button" class="rounded-xl!" [class.border-blue-500]="type() === 'MATERIAL'" [class.bg-blue-50]="type() === 'MATERIAL'" [attr.aria-selected]="type() === 'MATERIAL'" (click)="selectType('MATERIAL')">Materiales</button> }
          @if (canUploadProduct()) { <button mat-stroked-button type="button" class="rounded-xl!" [class.border-blue-500]="type() === 'PRODUCTO'" [class.bg-blue-50]="type() === 'PRODUCTO'" [attr.aria-selected]="type() === 'PRODUCTO'" (click)="selectType('PRODUCTO')">Productos</button> }
          @if (canUploadClient()) { <button mat-stroked-button type="button" class="rounded-xl!" [class.border-blue-500]="type() === 'CLIENTE'" [class.bg-blue-50]="type() === 'CLIENTE'" [attr.aria-selected]="type() === 'CLIENTE'" (click)="selectType('CLIENTE')">Clientes</button> }
          @if (canUploadProvider()) { <button mat-stroked-button type="button" class="rounded-xl!" [class.border-blue-500]="type() === 'PROVEEDOR'" [class.bg-blue-50]="type() === 'PROVEEDOR'" [attr.aria-selected]="type() === 'PROVEEDOR'" (click)="selectType('PROVEEDOR')">Proveedores</button> }
          @if (canUploadAgent()) { <button mat-stroked-button type="button" class="rounded-xl!" [class.border-blue-500]="type() === 'AGENTE'" [class.bg-blue-50]="type() === 'AGENTE'" [attr.aria-selected]="type() === 'AGENTE'" (click)="selectType('AGENTE')">Agentes aduanales</button> }
        </div>

        <div class="flex flex-col gap-3 rounded-xl border border-dashed border-slate-300 bg-slate-50 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div><h2 class="m-0 text-sm font-semibold text-slate-800">Selecciona un archivo</h2><p class="mb-0 mt-1 text-xs text-slate-500">.xls o .xlsx · máximo 10 MiB · contrato derivado de los stages legacy</p></div>
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
          @if (current.filasInvalidas > 0) {
            <section class="mt-5 rounded-xl border border-red-200 bg-red-50 p-4" aria-label="Errores">
              <div class="flex flex-wrap items-center justify-between gap-3"><div><h2 class="m-0 text-sm font-semibold text-red-900">Errores de validación</h2><p class="mb-0 mt-1 text-sm text-red-800">Descarga todos los errores persistidos de esta carga.</p></div><button mat-stroked-button type="button" [disabled]="isDownloadingErrors()" (click)="downloadErrors()">@if (isDownloadingErrors()) { Descargando errores... } @else { Descargar errores }</button></div>
              @for (issue of current.errores; track $index) { <p class="mb-0 mt-2 text-sm text-red-800">{{ issue.fila ? 'Fila ' + issue.fila + ' · ' : '' }}{{ issue.columna || 'Estructura' }} · {{ issue.mensaje }}</p> }
            </section>
          }
          @if (previewError()) { <div class="mt-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert"><p class="m-0 font-semibold">No pudimos cambiar la página</p><p class="mb-3 mt-1">{{ previewError() }}</p><button mat-stroked-button type="button" (click)="retryPreviewPage()">Reintentar</button></div> }
          @if (isPreviewLoading()) { <div class="mt-5 h-24 animate-pulse rounded-xl bg-slate-100" aria-label="Cargando página de previsualización" aria-busy="true"></div> } @else if (current.filas.length) { <div class="mt-5 overflow-x-auto rounded-xl border border-slate-200"><table class="w-full min-w-max text-left text-xs"><thead class="bg-slate-50"><tr>@for (column of current.columnas; track column) { <th class="whitespace-nowrap px-3 py-3 font-semibold text-slate-600">{{ column }}</th> }</tr></thead><tbody class="divide-y divide-slate-100">@for (row of current.filas; track $index) { <tr>@for (column of current.columnas; track column) { <td class="whitespace-nowrap px-3 py-3 text-slate-600">{{ row[column] || '—' }}</td> }</tr> }</tbody></table></div> }
          @if (current.totalPersistido > previewPageSize()) { <mat-paginator class="mt-3" [length]="current.totalPersistido" [pageIndex]="previewPage() - 1" [pageSize]="previewPageSize()" [pageSizeOptions]="[25, 50, 100]" showFirstLastButtons aria-label="Paginación de previsualización" (page)="changePreviewPage($event)" /> }
          @if (current.estado === 'CONFIRMADA') {
            <p class="mt-5 rounded-xl border border-green-200 bg-green-50 p-4 text-sm text-green-900" role="status">
              Importación confirmada correctamente.
              @if (confirmation()?.confirmadaEn) { <span> Confirmada en: {{ confirmation()!.confirmadaEn }}.</span> }
            </p>
          } @else if (canConfirmCurrent()) {
            <section class="mt-5 rounded-xl border border-amber-200 bg-amber-50 p-4" aria-label="Confirmación de importación">
              <p class="m-0 text-sm text-amber-950">{{ confirmationAdvertencia() }}</p>
              @if (confirmationError()) { <p class="mb-0 mt-3 text-sm text-red-800" role="alert">{{ confirmationError() }}</p> }
              <button mat-flat-button type="button" class="mt-4" [disabled]="isConfirming()" (click)="requestConfirmation()">
                @if (isConfirming()) { Confirmando importación... } @else { Confirmar importación }
              </button>
            </section>
          } @else {
            <p class="mt-5 rounded-xl border border-blue-100 bg-blue-50 p-4 text-sm text-blue-900">Previsualización validada. La confirmación hacia CALE_IMMEX todavía no está habilitada.</p>
          }
        }
      </section>
    </main>
  `,
})
export class CatalogImportPage {
  private readonly auth = inject(AuthService);
  protected readonly canUploadMaterial = computed(() => this.auth.hasPermission('MATERIALES_CARGAR'));
  protected readonly canUploadProduct = computed(() => this.auth.hasPermission('PRODUCTOS_CARGAR'));
  protected readonly canUploadClient = computed(() => this.auth.hasPermission('CLIENTES_CARGAR'));
  protected readonly canUploadProvider = computed(() => this.auth.hasPermission('PROVEEDORES_CARGAR'));
  protected readonly canUploadAgent = computed(() => this.auth.hasPermission('AGENTES_CARGAR'));
  protected readonly canConfirmMaterial = computed(() => this.auth.hasPermission('MATERIALES_CONFIRMAR'));
  protected readonly canConfirmProduct = computed(() => this.auth.hasPermission('PRODUCTOS_CONFIRMAR'));
  protected readonly canConfirmClient = computed(() => this.auth.hasPermission('CLIENTES_CONFIRMAR'));
  protected readonly canConfirmProvider = computed(() => this.auth.hasPermission('PROVEEDORES_CONFIRMAR'));
  protected readonly canConfirmAgent = computed(() => this.auth.hasPermission('AGENTES_CONFIRMAR'));
  protected readonly type = signal<CatalogImportType>(this.resolveInitialType());
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly selectionError = signal<string | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly isPreviewLoading = signal(false);
  protected readonly isDownloadingErrors = signal(false);
  protected readonly previewPage = signal(1);
  protected readonly previewPageSize = signal(100);
  protected readonly previewError = signal<string | null>(null);
  protected readonly isConfirming = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly confirmationError = signal<string | null>(null);
  protected readonly confirmation = signal<CatalogMaterialImportConfirmation | CatalogProductImportConfirmation | CatalogClientImportConfirmation | CatalogProviderImportConfirmation | CatalogAgentImportConfirmation | null>(null);
  protected readonly result = signal<CatalogImportResponse | null>(null);
  protected readonly canConfirmCurrent = computed(() => {
    const current = this.result();
    if (!current || current.estado !== 'PREVISUALIZADA') return false;
    switch (current.tipo) {
      case 'MATERIAL': return this.canConfirmMaterial();
      case 'PRODUCTO': return this.canConfirmProduct();
      case 'CLIENTE': return this.canConfirmClient();
      case 'PROVEEDOR': return this.canConfirmProvider();
      case 'AGENTE': return this.canConfirmAgent();
    }
  });
  protected readonly confirmationAdvertencia = computed(() => {
    const current = this.result();
    if (current?.tipo === 'PRODUCTO' || current?.tipo === 'CLIENTE' || current?.tipo === 'PROVEEDOR' || current?.tipo === 'AGENTE') {
      return 'La confirmación incorpora únicamente los registros válidos nuevos; los existentes con la misma clave se mantienen sin cambios.';
    }
    return 'La confirmación actualizará el catálogo de materiales utilizando las reglas actuales del sistema Anexo 24.';
  });
  private readonly upload = inject(UploadCatalogImportUseCase);
  private readonly getPreview = inject(GetCatalogImportPreviewUseCase);
  private readonly errorCsv = inject(CatalogImportErrorCsvService);
  private readonly confirmMaterial = inject(ConfirmCatalogMaterialImportUseCase);
  private readonly confirmProduct = inject(ConfirmCatalogProductImportUseCase);
  private readonly confirmClient = inject(ConfirmCatalogClientImportUseCase);
  private readonly confirmProvider = inject(ConfirmCatalogProviderImportUseCase);
  private readonly confirmAgent = inject(ConfirmCatalogAgentImportUseCase);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  private resolveInitialType(): CatalogImportType {
    if (this.auth.hasPermission('MATERIALES_CARGAR')) return 'MATERIAL';
    if (this.auth.hasPermission('PRODUCTOS_CARGAR')) return 'PRODUCTO';
    if (this.auth.hasPermission('CLIENTES_CARGAR')) return 'CLIENTE';
    if (this.auth.hasPermission('PROVEEDORES_CARGAR')) return 'PROVEEDOR';
    if (this.auth.hasPermission('AGENTES_CARGAR')) return 'AGENTE';
    throw new Error('La ruta de importaciones requiere un permiso de carga.');
  }

  selectType(type: CatalogImportType): void {
    this.type.set(type); this.selectedFile.set(null); this.selectionError.set(null); this.error.set(null); this.previewError.set(null); this.confirmationError.set(null); this.confirmation.set(null); this.result.set(null); this.previewPage.set(1); this.previewPageSize.set(100);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectionError.set(null); this.error.set(null); this.previewError.set(null); this.confirmationError.set(null); this.confirmation.set(null); this.result.set(null); this.previewPage.set(1); this.previewPageSize.set(100);
    if (!file) return;
    if (file.size > 10 * 1024 * 1024 || !/\.(xls|xlsx)$/i.test(file.name)) {
      this.selectedFile.set(null); this.selectionError.set('Selecciona un archivo .xls o .xlsx de máximo 10 MiB.'); return;
    }
    this.selectedFile.set(file); this.submit(file);
  }

  retry(): void { const file = this.selectedFile(); if (file) this.submit(file); }

  protected changePreviewPage(event: PageEvent): void {
    const current = this.result();
    if (!current) return;
    this.previewPage.set(event.pageIndex + 1);
    this.previewPageSize.set(event.pageSize);
    this.loadPreview(current);
  }

  protected retryPreviewPage(): void {
    const current = this.result();
    if (current) this.loadPreview(current);
  }

  protected downloadErrors(): void {
    const current = this.result();
    if (!current || current.filasInvalidas === 0 || this.isDownloadingErrors()) return;
    this.isDownloadingErrors.set(true);
    this.errorCsv.download(current.tipo, current.id).subscribe({
      next: () => {
        this.isDownloadingErrors.set(false);
        this.notifications.success('Errores de validación descargados.');
      },
      error: (err: { message?: string; error?: { message?: string } }) => {
        this.isDownloadingErrors.set(false);
        this.notifications.error(err?.error?.message || err?.message || 'No fue posible descargar los errores de validación.');
      },
    });
  }

  protected requestConfirmation(): void {
    const current = this.result();
    if (!current || !this.canConfirmCurrent() || this.isConfirming()) return;
    const titulo = `Confirmar importación de ${this.tituloTipo(current.tipo)}`;
    this.confirm.ask({
      title: titulo,
      message: this.confirmationAdvertencia(),
      confirmLabel: 'Confirmar importación',
      kind: 'warning',
    }).subscribe((accepted) => {
      if (!accepted) return;
      this.confirmationError.set(null);
      this.isConfirming.set(true);
      const stream = this.streamConfirmacion(current.tipo, current.id);
      stream.subscribe({
        next: (confirmation: CatalogMaterialImportConfirmation | CatalogProductImportConfirmation | CatalogClientImportConfirmation | CatalogProviderImportConfirmation | CatalogAgentImportConfirmation) => {
          this.isConfirming.set(false);
          this.confirmation.set(confirmation);
          this.result.update((latest) => latest && latest.id === current.id ? {
            ...latest,
            estado: confirmation.estado,
            totalFilas: confirmation.totalFilas,
            filasValidas: confirmation.filasValidas,
            filasInvalidas: confirmation.filasConError,
          } : latest);
          this.notifications.success(this.mensajeConfirmacion(current.tipo));
        },
        error: (err: { error?: { message?: string } }) => {
          this.isConfirming.set(false);
          const mensaje = this.mensajeErrorConfirmacion(current.tipo);
          this.confirmationError.set(err?.error?.message || mensaje);
          this.notifications.error(mensaje);
        },
      });
    });
  }

  private tituloTipo(tipo: CatalogImportType): string {
    switch (tipo) {
      case 'MATERIAL': return 'materiales';
      case 'PRODUCTO': return 'productos';
      case 'CLIENTE': return 'clientes';
      case 'PROVEEDOR': return 'proveedores';
      case 'AGENTE': return 'agentes aduanales';
    }
  }

  private mensajeConfirmacion(tipo: CatalogImportType): string {
    return `Importación de ${this.tituloTipo(tipo)} confirmada correctamente.`;
  }

  private mensajeErrorConfirmacion(tipo: CatalogImportType): string {
    return `No fue posible confirmar la importación de ${this.tituloTipo(tipo)}.`;
  }

  private streamConfirmacion(tipo: CatalogImportType, cargaId: number): Observable<CatalogMaterialImportConfirmation | CatalogProductImportConfirmation | CatalogClientImportConfirmation | CatalogProviderImportConfirmation | CatalogAgentImportConfirmation> {
    switch (tipo) {
      case 'MATERIAL': return this.confirmMaterial.execute(cargaId);
      case 'PRODUCTO': return this.confirmProduct.execute(cargaId);
      case 'CLIENTE': return this.confirmClient.execute(cargaId);
      case 'PROVEEDOR': return this.confirmProvider.execute(cargaId);
      case 'AGENTE': return this.confirmAgent.execute(cargaId);
    }
  }

  private loadPreview(current: CatalogImportResponse): void {
    this.isPreviewLoading.set(true);
    this.previewError.set(null);
    this.getPreview.execute(current.tipo, current.id, this.previewPage(), this.previewPageSize()).subscribe({
      next: (response) => {
        this.result.set(response);
        this.isPreviewLoading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.previewError.set(err?.error?.message || 'No fue posible cargar esta página de la previsualización.');
        this.isPreviewLoading.set(false);
        this.notifications.error('No fue posible cargar la página de previsualización.');
      },
    });
  }

  private submit(file: File): void {
    this.isLoading.set(true); this.error.set(null); this.confirmationError.set(null); this.confirmation.set(null);
    this.upload.execute(this.type(), file).subscribe({
      next: response => { this.result.set(response); this.previewPage.set(1); this.isLoading.set(false); },
      error: err => { this.error.set(err?.error?.message || 'Revisa el archivo y vuelve a intentar.'); this.isLoading.set(false); this.notifications.error('No fue posible validar el archivo.'); },
    });
  }
}