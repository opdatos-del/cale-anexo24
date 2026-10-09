import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { catchError, finalize, of } from 'rxjs';
import { NotificationService } from '@core/notifications/notification.service';
import { SavedCriteria, SavedQuery, SavedQueryScope } from '@features/saved-queries/domain/saved-query.model';
import { SavedQueriesApi } from '@features/saved-queries/infrastructure/saved-queries.api';

/** Control reutilizable de presets; aplicar delega validación final a cada pantalla. */
@Component({
  selector: 'app-saved-queries-control',
  imports: [DatePipe, FormsModule, MatButtonModule, MatIconModule],
  template: `
    <section class="relative" aria-label="Consultas guardadas">
      <div class="flex flex-wrap gap-2">
        <button mat-stroked-button type="button" (click)="toggleList()" [disabled]="loading()" aria-label="Ver consultas guardadas"><mat-icon aria-hidden="true">bookmark</mat-icon>Consultas guardadas</button>
        <button mat-stroked-button type="button" (click)="openEditor()" [disabled]="loading()"><mat-icon aria-hidden="true">bookmark_add</mat-icon>Guardar consulta</button>
      </div>
      @if (editorOpen()) {
        <div class="mt-2 rounded-lg border border-slate-200 bg-white p-3 shadow-sm">
          <label class="block text-xs font-medium">Nombre<input class="mt-1 w-full rounded border p-2" maxlength="80" [(ngModel)]="name" aria-label="Nombre de consulta guardada" /></label>
          <label class="mt-2 block text-xs font-medium">Descripción<input class="mt-1 w-full rounded border p-2" maxlength="250" [(ngModel)]="description" aria-label="Descripción de consulta guardada" /></label>
          <div class="mt-3 flex gap-2"><button mat-flat-button type="button" (click)="save()" [disabled]="loading() || !name.trim() || !canSaveCriteria()">{{ editing() ? 'Actualizar' : 'Guardar' }}</button><button mat-button type="button" (click)="closeEditor()">Cancelar</button></div>
        </div>
      }
      @if (listOpen()) {
        <div class="mt-2 rounded-lg border border-slate-200 bg-white p-3 shadow-sm">
          @if (queries().length === 0) { <p class="m-0 text-sm text-slate-500">No hay consultas guardadas.</p> }
          @for (query of queries(); track query.id) {
            <div class="flex items-center justify-between gap-2 border-b py-2 last:border-0">
              <div><p class="m-0 text-sm font-medium">{{ query.nombre }}</p>@if (query.descripcion) { <p class="m-0 text-xs text-slate-500">{{ query.descripcion }}</p> }<p class="m-0 text-xs text-slate-400">{{ query.alcance }} · Actualizada {{ query.fechaActualizacion | date:'short' }}</p></div>
              <div class="flex gap-1"><button mat-button type="button" (click)="apply(query)" [disabled]="loading()" [attr.aria-label]="'Aplicar ' + query.nombre">Aplicar</button><button mat-icon-button type="button" (click)="openEditor(query)" [disabled]="loading()" [attr.aria-label]="'Editar ' + query.nombre"><mat-icon>edit</mat-icon></button><button mat-icon-button type="button" (click)="remove(query)" [disabled]="loading()" [attr.aria-label]="'Eliminar ' + query.nombre"><mat-icon>delete</mat-icon></button></div>
            </div>
          }
        </div>
      }
    </section>
  `,
})
export class SavedQueriesControlComponent {
  @Input({ required: true }) scope!: SavedQueryScope;
  @Input({ required: true }) criteria!: SavedCriteria;
  @Output() readonly applyCriteria = new EventEmitter<SavedCriteria>();
  protected readonly queries = signal<SavedQuery[]>([]);
  protected readonly loading = signal(false);
  protected readonly listOpen = signal(false);
  protected readonly editorOpen = signal(false);
  protected readonly editing = signal<SavedQuery | null>(null);
  protected name = '';
  protected description = '';
  private readonly api = inject(SavedQueriesApi);
  private readonly notifications = inject(NotificationService);

  protected canSaveCriteria(): boolean {
    if (!isSafeCriteria(this.criteria)) return false;
    const from = this.criteria['from'];
    const to = this.criteria['to'];
    const hasFrom = typeof from === 'string' && from.length > 0;
    const hasTo = typeof to === 'string' && to.length > 0;
    if (this.scope === 'ENTRADAS' || this.scope === 'SALIDAS' || this.scope === 'MATERIALES_UTILIZADOS') {
      return hasFrom && hasTo && from <= to;
    }
    if (this.scope === 'ACTIVOS_FIJOS' || this.scope === 'REPORTES') {
      if (this.scope === 'REPORTES' && typeof this.criteria['type'] !== 'string') return false;
      return hasFrom === hasTo && (!hasFrom || from! <= to!);
    }
    return false;
  }

  protected toggleList(): void { this.listOpen.update((open) => !open); if (this.listOpen()) this.load(); }
  protected openEditor(query?: SavedQuery): void { this.editing.set(query ?? null); this.name = query?.nombre ?? ''; this.description = query?.descripcion ?? ''; this.editorOpen.set(true); }
  protected closeEditor(): void { this.editorOpen.set(false); this.editing.set(null); }
  protected apply(query: SavedQuery): void { if (isSafeCriteria(query.criterios)) this.applyCriteria.emit(query.criterios); else this.notifications.error('Esta consulta guardada ya no es compatible con esta pantalla.'); }
  protected save(): void {
    if (!this.canSaveCriteria()) { this.notifications.error('No fue posible guardar la consulta.'); return; }
    const request = { nombre: this.name.trim(), descripcion: this.description.trim() || null, alcance: this.scope, criterios: this.criteria };
    if (!request.nombre || !isSafeCriteria(request.criterios)) { this.notifications.error('No fue posible guardar la consulta.'); return; }
    this.loading.set(true);
    const editing = this.editing();
    (editing ? this.api.update(editing.id, request) : this.api.create(request)).pipe(catchError((error: unknown) => { this.notifications.error(saveErrorMessage(error, Boolean(editing))); return of(null); }), finalize(() => this.loading.set(false))).subscribe((query) => { if (!query) return; this.closeEditor(); this.load(); this.notifications.success('Consulta guardada.'); });
  }
  protected remove(query: SavedQuery): void {
    if (!confirm(`¿Eliminar la consulta guardada "${query.nombre}"?`)) return;
    this.loading.set(true);
    this.api.delete(query.id).pipe(catchError(() => { this.notifications.error('No fue posible eliminar la consulta.'); return of(undefined); }), finalize(() => this.loading.set(false))).subscribe(() => this.load());
  }
  private load(): void { if (!this.scope) return; this.loading.set(true); this.api.list(this.scope).pipe(catchError(() => { this.notifications.error('No fue posible cargar las consultas guardadas.'); return of([]); }), finalize(() => this.loading.set(false))).subscribe((queries) => this.queries.set(queries)); }
}

function isSafeCriteria(value: unknown): value is SavedCriteria {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
    && Object.values(value as Record<string, unknown>).every((item) => item === null || typeof item === 'string' || typeof item === 'number');
}

function saveErrorMessage(error: unknown, editing: boolean): string {
  if (!(error instanceof HttpErrorResponse) || error.status !== 409) return 'No fue posible guardar la consulta.';
  const body = error.error as { code?: unknown } | null;
  if (body?.code === 'RECURSO_DUPLICADO') return 'Ya existe una consulta guardada con ese nombre.';
  if (!editing && body?.code === 'ESTADO_INCOMPATIBLE') return 'Alcanzaste el máximo de 100 consultas guardadas.';
  return 'No fue posible guardar la consulta.';
}
