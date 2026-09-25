import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { EMPTY, catchError, finalize } from 'rxjs';
import { userFacingApiError } from '@core/http/api-error.util';
import { UploadBillingFilesUseCase } from '@features/billing/application/use-cases/upload-billing-files.use-case';
import { BillingLoad, BillingTemplate, BillingUploadResponse } from '@features/billing/domain/models/billing-upload.model';
import { BillingApiService } from '@features/billing/infrastructure/api/billing-api.service';

const MAX_FILES = 5;
const MAX_FILE_SIZE = 10 * 1024 * 1024;

@Component({
  imports: [CommonModule, MatIconModule],
  selector: 'app-billing-upload',
  template: `
    <main class="mx-auto min-h-full w-full max-w-7xl bg-slate-50 px-4 py-7 text-slate-800 sm:px-7 lg:px-9">
      <header class="mb-6">
        <p class="mb-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Facturación · V1</p>
        <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Carga de facturación</h1>
        <p class="mt-1 text-sm text-slate-500">Carga archivos para validar sus registros antes de cualquier confirmación.</p>
      </header>

      <section class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6" aria-label="Carga de archivos de facturación">
        <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div><h2 class="m-0 text-base font-semibold text-slate-800">Selecciona archivos</h2><p class="mb-0 mt-1 text-xs text-slate-500">Excel .xls o .xlsx · máximo 5 archivos · 10 MiB por archivo</p></div>
          <button type="button" (click)="downloadTemplate()" [disabled]="!template() || isLoading()" class="inline-flex min-h-10 items-center justify-center gap-2 rounded-lg border border-blue-200 px-4 text-sm font-medium text-blue-700 hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-50" title="Descargar layout compatible"><mat-icon aria-hidden="true">description</mat-icon>Descargar layout</button>
        </div>

        <label class="mt-5 flex min-h-36 cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-slate-300 bg-slate-50 px-5 text-center transition hover:border-blue-400 hover:bg-blue-50/40" [class.pointer-events-none]="isLoading()" [class.opacity-60]="isLoading()">
          <mat-icon class="mb-2 text-blue-500" aria-hidden="true">upload_file</mat-icon>
          <span class="text-sm font-medium text-slate-700">Selecciona archivos desde tu equipo</span>
          <span class="mt-1 text-xs text-slate-500">No se aceptan otros formatos</span>
          <input class="sr-only" type="file" accept=".xls,.xlsx,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" multiple [disabled]="isLoading()" (change)="onFilesSelected($event)" aria-label="Seleccionar archivos Excel" />
        </label>
        @if (selectionMessage()) { <p class="mb-0 mt-3 text-sm text-amber-700" role="status">{{ selectionMessage() }}</p> }

        @if (selectedFiles().length) {
          <ul class="mt-4 divide-y divide-slate-100 rounded-xl border border-slate-200" aria-label="Archivos seleccionados">
            @for (file of selectedFiles(); track file.name + file.lastModified) {
              <li class="flex min-w-0 items-center justify-between gap-3 px-3 py-3 sm:px-4"><div class="flex min-w-0 items-center gap-3"><mat-icon class="shrink-0 text-emerald-600" aria-hidden="true">table_view</mat-icon><div class="min-w-0"><p class="m-0 truncate text-sm font-medium text-slate-700">{{ file.name }}</p><p class="m-0 text-xs text-slate-400">{{ formatSize(file.size) }}</p></div></div><span class="shrink-0 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700">Listo</span></li>
            }
          </ul>
          <div class="mt-4 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <button type="button" (click)="clearSelection()" [disabled]="isLoading()" class="min-h-10 rounded-lg px-4 text-sm font-medium text-slate-600 hover:bg-slate-100 disabled:opacity-50">Limpiar</button>
            <button type="button" (click)="upload()" [disabled]="isLoading() || !selectedFiles().length" class="inline-flex min-h-10 items-center justify-center gap-2 rounded-lg bg-blue-600 px-5 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"><mat-icon aria-hidden="true">cloud_upload</mat-icon>{{ isLoading() ? 'Validando…' : 'Cargar y validar' }}</button>
          </div>
        }
      </section>

      @if (isLoading()) { <section class="mt-5 flex min-h-28 items-center justify-center gap-3 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-600" aria-live="polite" aria-busy="true"><span class="h-5 w-5 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></span>Subiendo archivos y validando…</section> }
      @if (error()) { <section class="mt-5 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert"><p class="m-0 font-semibold">No se pudo completar la carga</p><p class="mb-0 mt-1">{{ error() }}</p></section> }

      @if (response(); as result) {
        <section class="mt-6" aria-label="Resultados de la validación">
          <div class="mb-4 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between"><div><h2 class="m-0 text-lg font-semibold text-slate-900">Resultados de validación</h2><p class="mb-0 mt-1 break-all text-xs text-slate-500">Referencia: {{ result.correlationId }}</p></div><span class="w-fit rounded-full bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-800">Validación solamente · sin confirmación</span></div>
          @for (load of result.cargas; track load.id) {
            <article class="mb-4 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
              <div class="flex flex-col gap-3 border-b border-slate-100 p-4 sm:flex-row sm:items-start sm:justify-between sm:px-5"><div class="min-w-0"><h3 class="m-0 break-all text-sm font-semibold text-slate-800">{{ load.archivo }}</h3><p class="mb-0 mt-1 break-all text-xs text-slate-400">Hash: {{ load.hash }}</p></div><span class="w-fit rounded-full px-3 py-1 text-xs font-semibold" [ngClass]="statusClass(load)">{{ statusLabel(load.estado) }}</span></div>
              <div class="grid grid-cols-2 gap-3 p-4 sm:grid-cols-4 sm:px-5"><div class="rounded-lg bg-slate-50 p-3"><span class="block text-xs text-slate-500">Total</span><strong>{{ load.totalRegistros }}</strong></div><div class="rounded-lg bg-emerald-50 p-3"><span class="block text-xs text-emerald-700">Válidos</span><strong>{{ load.registrosValidos }}</strong></div><div class="rounded-lg bg-red-50 p-3"><span class="block text-xs text-red-700">Inválidos</span><strong>{{ load.registrosInvalidos }}</strong></div><div class="rounded-lg bg-blue-50 p-3"><span class="block text-xs text-blue-700">Errores</span><strong>{{ load.errores.length }}</strong></div></div>
              @if (load.preview.columnas.length) {
                <div class="border-t border-slate-100 p-4 sm:px-5"><h4 class="mb-3 mt-0 text-sm font-semibold text-slate-700">Vista previa <span class="font-normal text-slate-400">(hasta las filas devueltas por el servidor)</span></h4>@if (load.preview.filas.length) { <div class="overflow-x-auto rounded-lg border border-slate-200"><table class="w-full min-w-max text-left text-xs"><thead class="bg-slate-50"><tr>@for (column of load.preview.columnas; track column) { <th class="whitespace-nowrap px-3 py-2 font-semibold text-slate-600">{{ column }}</th> }</tr></thead><tbody class="divide-y divide-slate-100">@for (row of load.preview.filas; track $index) { <tr>@for (column of load.preview.columnas; track column) { <td class="max-w-64 whitespace-nowrap px-3 py-2 text-slate-600">{{ row[column] ?? '—' }}</td> }</tr> }</tbody></table></div> } @else { <p class="m-0 text-xs text-slate-500">No hay filas disponibles para mostrar.</p> }</div>
              }
              @if (load.errores.length) { <div class="border-t border-slate-100 p-4 sm:px-5"><h4 class="mb-3 mt-0 text-sm font-semibold text-slate-700">Errores de validación</h4><div class="space-y-2">@for (issue of load.errores; track $index) { <div class="rounded-lg border border-red-100 bg-red-50/60 p-3"><p class="m-0 text-sm font-medium text-red-800">{{ issue.mensaje }}</p><p class="mb-0 mt-1 text-xs text-red-700">{{ errorLocation(issue) }} · {{ issue.codigo }}@if (issue.valorEnmascarado) { · Valor: {{ issue.valorEnmascarado }} }</p></div> }</div></div> }
            </article>
          } @empty { <div class="rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-10 text-center text-sm text-slate-500">La respuesta no contiene cargas.</div> }
          <p class="m-0 rounded-xl border border-blue-100 bg-blue-50 p-4 text-sm text-blue-900"><strong>Estructura {{ template()?.nombre ?? 'de Facturación' }} validada.</strong> Esta carga persiste una vista previa para revisión. La confirmación pendiente de validación del proceso de integración con Módulo C.</p>
        </section>
      } @else if (!isLoading() && !error()) {
        <section class="mt-5 flex min-h-40 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-8 text-center"><mat-icon class="mb-2 text-slate-300" aria-hidden="true">receipt_long</mat-icon><p class="m-0 text-sm font-medium text-slate-600">Aún no hay cargas para mostrar</p><span class="mt-1 text-xs text-slate-400">Selecciona uno o varios archivos Excel para comenzar.</span></section>
      }
    </main>
  `,
})
export class BillingUploadPage implements OnInit {
  protected readonly selectedFiles = signal<File[]>([]);
  protected readonly selectionMessage = signal<string | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly response = signal<BillingUploadResponse | null>(null);
  protected readonly template = signal<BillingTemplate | null>(null);
  private readonly uploadFiles = inject(UploadBillingFilesUseCase);
  private readonly billingApi = inject(BillingApiService);

  ngOnInit(): void {
    this.billingApi.template().pipe(catchError(() => EMPTY)).subscribe((template) => this.template.set(template));
  }

  protected downloadTemplate(): void {
    this.billingApi.downloadTemplate().subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = 'Layout_Facturas.xlsx';
      anchor.click();
      URL.revokeObjectURL(url);
    });
  }

  protected onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.addFiles(Array.from(input.files ?? []));
    input.value = '';
  }

  protected clearSelection(): void {
    this.selectedFiles.set([]);
    this.selectionMessage.set(null);
    this.error.set(null);
    this.response.set(null);
  }

  protected formatSize(size: number): string {
    return `${(size / 1024 / 1024).toFixed(2)} MiB`;
  }

  protected upload(): void {
    if (!this.selectedFiles().length || this.isLoading()) return;
    this.isLoading.set(true);
    this.error.set(null);
    this.response.set(null);
    this.uploadFiles.execute(this.selectedFiles()).pipe(
      catchError((cause: unknown) => {
        this.error.set(userFacingApiError(cause, 'Verifica tu conexión e inténtalo nuevamente.'));
        return EMPTY;
      }),
      finalize(() => this.isLoading.set(false)),
    ).subscribe((result) => this.response.set(result));
  }

  protected statusLabel(status: BillingLoad['estado']): string {
    return status === 'VALIDADA' ? 'Validada' : status === 'CON_ERRORES' ? 'Con errores' : 'Fallida';
  }

  protected statusClass(load: BillingLoad): string {
    return load.estado === 'VALIDADA' ? 'bg-emerald-50 text-emerald-700' : load.estado === 'CON_ERRORES' ? 'bg-amber-50 text-amber-800' : 'bg-red-50 text-red-700';
  }

  protected errorLocation(issue: BillingLoad['errores'][number]): string {
    return [issue.hoja, issue.fila === null ? null : `Fila ${issue.fila}`, issue.columna].filter(Boolean).join(' · ') || 'Ubicación no especificada';
  }

  private addFiles(incoming: File[]): void {
    const accepted = [...this.selectedFiles()];
    const messages: string[] = [];
    for (const file of incoming) {
      const extension = file.name.split('.').pop()?.toLowerCase();
      if (extension !== 'xls' && extension !== 'xlsx') {
        messages.push(`${file.name}: formato no permitido.`);
        continue;
      }
      if (file.size > MAX_FILE_SIZE) {
        messages.push(`${file.name}: supera el máximo de 10 MiB.`);
        continue;
      }
      if (accepted.some((selected) => selected.name === file.name && selected.size === file.size && selected.lastModified === file.lastModified)) continue;
      if (accepted.length >= MAX_FILES) {
        messages.push(`Sólo se permiten ${MAX_FILES} archivos por carga.`);
        continue;
      }
      accepted.push(file);
    }
    this.selectedFiles.set(accepted);
    this.selectionMessage.set(messages.length ? messages.join(' ') : null);
  }
}
