import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { EMPTY, catchError, finalize, switchMap } from 'rxjs';
import { AuthService } from '@core/auth/auth.service';
import { userFacingApiError } from '@core/http/api-error.util';
import { NotificationService } from '@core/notifications/notification.service';
import { ConfirmService } from '@core/ui/confirm-dialog/confirm.service';
import { ActaConfirmation, ActaError, ActaLoad, ActaLoadState } from '@features/operations/actas/domain/models/acta-import.model';
import { UploadActaUseCase } from '@features/operations/actas/application/use-cases/upload-acta.use-case';
import { GetActaUseCase } from '@features/operations/actas/application/use-cases/get-acta.use-case';
import { ConfirmActaUseCase } from '@features/operations/actas/application/use-cases/confirm-acta.use-case';

const MAX_FILE_SIZE = 10 * 1024 * 1024;
const PREVIEW_COLUMNS = ['Folio', 'Fecha', 'Clave', 'Linea', 'Cantidad', 'Umc', 'DescargaDirigida', 'ValorComercial'];

const CONFIRM_MESSAGE = 'La confirmación procesará el acta mediante las reglas vigentes del sistema Anexo 24 y puede generar salidas, partidas y descargas dirigidas. Verifica la información antes de continuar.';

@Component({
  imports: [CommonModule, MatButtonModule, MatIconModule],
  selector: 'app-acta-upload',
  template: `
    <main class="mx-auto min-h-full w-full max-w-7xl bg-slate-50 px-4 py-7 text-slate-800 sm:px-7 lg:px-9">
      <header class="mb-6">
        <p class="mb-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Operaciones · Actas V1</p>
        <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Carga, validación y confirmación de actas</h1>
        <p class="mt-1 text-sm text-slate-500">Staging aislado, validación y confirmación autorizada que reutiliza las reglas vigentes del sistema Anexo 24.</p>
      </header>

      <section class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6" aria-label="Carga de actas">
        <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div><h2 class="m-0 text-base font-semibold text-slate-800">Selecciona un archivo</h2><p class="mb-0 mt-1 text-xs text-slate-500">Excel .xls o .xlsx · máximo 10 MiB · contrato derivado de dbo.ACTA (8 columnas)</p></div>
        </div>
        <label class="mt-5 flex min-h-36 cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-slate-300 bg-slate-50 px-5 text-center transition hover:border-blue-400 hover:bg-blue-50/40" [class.pointer-events-none]="isBusy()" [class.opacity-60]="isBusy()">
          <mat-icon class="mb-2 text-blue-500" aria-hidden="true">upload_file</mat-icon>
          <span class="text-sm font-medium text-slate-700">Selecciona un archivo de actas</span>
          <span class="mt-1 text-xs text-slate-500">Columnas: Folio, Fecha, Clave, Línea, Cantidad, Umc, Descarga dirigida y Valor comercial.</span>
          <input class="sr-only" type="file" accept=".xls,.xlsx,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" [disabled]="isBusy()" (change)="onFileSelected($event)" aria-label="Seleccionar archivo de actas" />
        </label>
        @if (selectionMessage()) { <p class="mb-0 mt-3 text-sm text-amber-700" role="status">{{ selectionMessage() }}</p> }
        @if (selectedFile(); as file) {
          <div class="mt-4 flex flex-col gap-3 rounded-xl border border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between"><div class="min-w-0"><p class="m-0 truncate text-sm font-medium text-slate-700">{{ file.name }}</p><p class="m-0 text-xs text-slate-400">{{ formatSize(file.size) }}</p></div><div class="flex gap-2"><button type="button" (click)="clear()" [disabled]="isBusy()" class="min-h-10 rounded-lg px-4 text-sm text-slate-600 hover:bg-slate-100">Limpiar</button><button type="button" (click)="upload()" [disabled]="isBusy()" class="inline-flex min-h-10 items-center justify-center gap-2 rounded-lg bg-blue-600 px-5 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"><mat-icon aria-hidden="true">cloud_upload</mat-icon>{{ isLoading() ? 'Validando…' : 'Cargar y validar' }}</button></div></div>
        }
      </section>

      @if (isLoading()) { <section class="mt-5 flex min-h-24 items-center justify-center gap-3 rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-600" aria-live="polite" aria-busy="true"><span class="h-5 w-5 animate-spin rounded-full border-2 border-blue-600 border-t-transparent"></span>Procesando archivo y guardando preview…</section> }
      @if (error()) { <section class="mt-5 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-800" role="alert"><p class="m-0 font-semibold">No se pudo procesar el archivo</p><p class="mb-0 mt-1">{{ error() }}</p></section> }

      @if (load(); as result) {
        <section class="mt-6" aria-label="Resultado de validación">
          <div class="mb-4 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between"><div><h2 class="m-0 text-lg font-semibold text-slate-900">Resultado de validación</h2><p class="mb-0 mt-1 break-all text-xs text-slate-500">{{ result.archivo }}</p></div><span class="w-fit rounded-full px-3 py-1 text-xs font-semibold" [class.bg-emerald-50]="result.estado === 'PREVISUALIZADA'" [class.text-emerald-700]="result.estado === 'PREVISUALIZADA'" [class.bg-amber-50]="result.estado === 'CON_ERRORES'" [class.text-amber-800]="result.estado === 'CON_ERRORES'" [class.bg-blue-50]="result.estado === 'CONFIRMADA'" [class.text-blue-700]="result.estado === 'CONFIRMADA'">{{ stateLabel(result.estado) }}</span></div>
          <div class="grid grid-cols-2 gap-3 sm:grid-cols-4"><div class="rounded-xl bg-white p-4 shadow-sm ring-1 ring-slate-200"><span class="block text-xs text-slate-500">Total</span><strong>{{ result.totalFilas }}</strong></div><div class="rounded-xl bg-emerald-50 p-4"><span class="block text-xs text-emerald-700">Válidas</span><strong>{{ result.filasValidas }}</strong></div><div class="rounded-xl bg-red-50 p-4"><span class="block text-xs text-red-700">Inválidas</span><strong>{{ result.filasInvalidas }}</strong></div><div class="rounded-xl bg-blue-50 p-4"><span class="block text-xs text-blue-700">Errores</span><strong>{{ result.errores.length }}</strong></div></div>
          @if (result.filas.length) { <div class="mt-5 overflow-x-auto rounded-2xl border border-slate-200 bg-white shadow-sm"><table class="w-full min-w-max text-left text-xs"><thead class="bg-slate-50"><tr>@for (column of previewColumns(result); track column) { <th class="whitespace-nowrap px-3 py-3 font-semibold text-slate-600">{{ column }}</th> }</tr></thead><tbody class="divide-y divide-slate-100">@for (row of result.filas; track $index) { <tr>@for (column of previewColumns(result); track column) { <td class="max-w-64 whitespace-nowrap px-3 py-3 text-slate-600">{{ row[column] || '—' }}</td> }</tr> }</tbody></table></div> } @else { <div class="mt-5 rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-10 text-center text-sm text-slate-500">No hay filas para mostrar.</div> }
          @if (result.errores.length) { <section class="mt-5 rounded-2xl border border-red-200 bg-red-50/60 p-4" aria-label="Errores de validación"><h3 class="m-0 text-sm font-semibold text-red-900">Errores de validación</h3><div class="mt-3 space-y-2">@for (issue of result.errores; track $index) { <div class="rounded-lg border border-red-100 bg-white p-3"><p class="m-0 text-sm font-medium text-red-800">{{ issue.mensaje }}</p><p class="mb-0 mt-1 text-xs text-red-700">{{ errorLocation(issue) }} · {{ issue.codigo }}</p></div> }</div></section> }
          @if (canConfirm()) {
            <div class="mt-5 flex flex-col gap-3 rounded-2xl border border-blue-100 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between">
              <div><p class="m-0 text-sm font-medium text-slate-700">Confirmación autoritativa</p><p class="mb-0 mt-1 text-xs text-slate-500">Aplicará las reglas vigentes del sistema Anexo 24 sobre la carga validada.</p></div>
              <button type="button" (click)="confirm()" [disabled]="isConfirming()" class="inline-flex min-h-10 items-center justify-center gap-2 rounded-lg bg-blue-600 px-5 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"><mat-icon aria-hidden="true">verified</mat-icon>{{ isConfirming() ? 'Confirmando…' : 'Confirmar' }}</button>
            </div>
          }
          @if (confirmation(); as summary) {
            <p class="mt-4 rounded-xl border border-slate-200 bg-white p-4 text-xs text-slate-600">Filas confirmadas: {{ summary.totalFilas }} · Confirmada: {{ summary.confirmadaEn ?? '—' }}</p>
          }
          <p class="mt-5 rounded-xl border border-blue-100 bg-blue-50 p-4 text-sm text-blue-900">{{ stateNote(result.estado) }}</p>
        </section>
      } @else if (!isLoading() && !error()) { <section class="mt-5 flex min-h-36 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white px-6 py-8 text-center"><mat-icon class="mb-2 text-slate-300" aria-hidden="true">description</mat-icon><p class="m-0 text-sm font-medium text-slate-600">Aún no hay cargas para mostrar</p><span class="mt-1 text-xs text-slate-400">Elige un archivo de actas autorizado para comenzar.</span></section> }
    </main>
  `,
})
export class ActaUploadPage {
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly selectionMessage = signal<string | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly isConfirming = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly load = signal<ActaLoad | null>(null);
  protected readonly confirmation = signal<ActaConfirmation | null>(null);
  protected readonly isBusy = computed(() => this.isLoading() || this.isConfirming());
  protected readonly canConfirm = computed(() => {
    const result = this.load();
    return this.auth.hasPermission('ACTAS_CONFIRMAR')
      && result !== null
      && result.estado === 'PREVISUALIZADA'
      && result.errores.length === 0
      && result.filasInvalidas === 0
      && !this.isConfirming();
  });
  private readonly auth = inject(AuthService);
  private readonly confirmService = inject(ConfirmService);
  private readonly confirmUseCase = inject(ConfirmActaUseCase);
  private readonly getUseCase = inject(GetActaUseCase);
  private readonly uploadUseCase = inject(UploadActaUseCase);
  private readonly notifications = inject(NotificationService);

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file) return;
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (extension !== 'xls' && extension !== 'xlsx') {
      this.selectedFile.set(null);
      this.selectionMessage.set('Sólo se permiten archivos .xls y .xlsx.');
      return;
    }
    if (file.size > MAX_FILE_SIZE) {
      this.selectedFile.set(null);
      this.selectionMessage.set('El archivo supera el máximo de 10 MiB.');
      return;
    }
    this.selectedFile.set(file);
    this.selectionMessage.set(null);
    this.error.set(null);
    this.load.set(null);
  }

  protected clear(): void {
    if (this.isConfirming()) return;
    this.selectedFile.set(null);
    this.selectionMessage.set(null);
    this.error.set(null);
    this.load.set(null);
    this.confirmation.set(null);
  }

  protected upload(): void {
    const file = this.selectedFile();
    if (!file || this.isBusy()) return;
    this.isLoading.set(true);
    this.error.set(null);
    this.confirmation.set(null);
    this.uploadUseCase.execute(file).pipe(catchError((cause: unknown) => {
      this.error.set(userFacingApiError(cause, 'Verifica tu conexión e inténtalo nuevamente.'));
      this.notifications.error('No fue posible validar el acta.');
      return EMPTY;
    }), finalize(() => this.isLoading.set(false))).subscribe((result) => this.load.set(result));
  }

  /** Confirma la carga tras confirmación explícita del usuario. */
  protected confirm(): void {
    const result = this.load();
    if (!result || !this.canConfirm() || this.isConfirming()) return;
    this.confirmService.ask({
      title: 'Confirmar acta',
      message: CONFIRM_MESSAGE,
      confirmLabel: 'Confirmar',
      cancelLabel: 'Cancelar',
      kind: 'warning',
    }).pipe(
      switchMap((aceptado) => {
        if (!aceptado) return EMPTY;
        this.isConfirming.set(true);
        return this.confirmUseCase.execute(result.id).pipe(catchError((cause: unknown) => {
          this.notifications.error(userFacingApiError(cause, 'No fue posible confirmar la carga.'));
          return EMPTY;
        }), finalize(() => this.isConfirming.set(false)));
      }),
      switchMap((confirmacion) => {
        if (!confirmacion) return EMPTY;
        this.confirmation.set(confirmacion);
        this.notifications.success('Acta confirmada.');
        return this.getUseCase.execute(result.id);
      }),
      catchError((cause: unknown) => {
        this.notifications.error(userFacingApiError(cause, 'No fue posible actualizar la carga.'));
        return EMPTY;
      }),
    ).subscribe((actualizada) => this.load.set(actualizada));
  }

  protected stateLabel(estado: ActaLoadState): string {
    if (estado === 'CONFIRMADA') return 'Confirmada';
    return estado === 'CON_ERRORES' ? 'Con errores' : 'Lista para confirmar';
  }

  protected stateNote(estado: ActaLoadState): string {
    if (estado === 'CONFIRMADA') return 'Carga confirmada. La operación autoritativa quedó registrada; no requiere más acciones.';
    if (estado === 'CON_ERRORES') return 'Corrige los errores y vuelve a cargar el archivo antes de confirmar.';
    return 'Previsualización validada. La confirmación reutiliza las reglas vigentes del sistema Anexo 24.';
  }

  protected previewColumns(load: ActaLoad): string[] {
    return PREVIEW_COLUMNS.filter((column) => load.columnas.includes(column));
  }

  protected errorLocation(error: ActaError): string {
    return [error.hoja, error.fila === null ? null : `Fila ${error.fila}`, error.columna].filter(Boolean).join(' · ') || 'Ubicación no especificada';
  }

  protected formatSize(size: number): string {
    return `${(size / 1024 / 1024).toFixed(2)} MiB`;
  }
}
