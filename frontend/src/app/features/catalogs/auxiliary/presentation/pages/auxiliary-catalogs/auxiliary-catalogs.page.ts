import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { Observable } from 'rxjs';
import { AppAlertComponent } from '@core/ui/app-alert/app-alert.component';
import { NotificationService } from '@core/notifications/notification.service';
import { userFacingApiError } from '@core/http/api-error.util';
import { SearchAuxiliaryCatalogUseCase } from '@features/catalogs/auxiliary/application/use-cases/search-auxiliary-catalog.use-case';
import {
  AuxiliaryCatalogKey,
  AuxiliaryPage,
  AuxiliarySearchCriteria,
  AuxiliaryTableRow,
} from '@features/catalogs/auxiliary/domain/models/auxiliary-catalog.model';

interface CatalogOption {
  key: AuxiliaryCatalogKey;
  label: string;
  description: string;
}

/** Superficie agrupada para los catálogos auxiliares read-only confirmados. */
@Component({
  imports: [AppAlertComponent, FormsModule, MatButtonModule, MatIconModule, MatInputModule, MatPaginatorModule, MatTableModule],
  selector: 'app-auxiliary-catalogs',
  template: `
    <div class="min-h-full bg-[#f4f7fb] text-slate-800">
      <main class="mx-auto w-full max-w-360 px-5 py-8 sm:px-8">
        <div class="mb-7">
          <p class="mb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Catálogos</p>
          <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Catálogos auxiliares</h1>
          <p class="mt-2 text-sm text-slate-500">Consulta de unidades, tipos de material, almacenes y categorías.</p>
        </div>

        <section class="mb-6 rounded-2xl border border-slate-200/80 bg-white p-3 shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-label="Seleccionar catálogo auxiliar">
          <div class="flex flex-wrap gap-2">
            @for (catalog of catalogOptions; track catalog.key) {
              <button
                mat-stroked-button
                type="button"
                class="rounded-xl!"
                [class.bg-blue-50]="selectedCatalog() === catalog.key"
                [class.border-blue-500]="selectedCatalog() === catalog.key"
                [attr.aria-pressed]="selectedCatalog() === catalog.key"
                (click)="selectCatalog(catalog.key)"
              >{{ catalog.label }}</button>
            }
          </div>
        </section>

        <section class="mb-6 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-[0_4px_18px_rgb(15_23_42/4%)]" aria-labelledby="filtros-auxiliares">
          <h2 id="filtros-auxiliares" class="sr-only">Filtros de catálogos auxiliares</h2>
          <form (submit)="search()" class="flex flex-wrap items-end gap-4">
            <label class="min-w-0 flex-1 sm:min-w-70">
              <span class="mb-1 block text-xs font-medium text-slate-700">Buscar en {{ selectedLabel() }}</span>
              <input matInput name="filter" [(ngModel)]="filter" placeholder="Clave, nombre o descripción" class="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-4 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10" />
            </label>
            <button mat-flat-button color="primary" type="submit" class="h-11 min-w-28 rounded-xl!">Consultar</button>
            <button mat-stroked-button type="button" class="h-11 min-w-24 rounded-xl!" (click)="clearFilter()">Limpiar</button>
          </form>
        </section>

        <div class="mb-4 flex flex-wrap items-center gap-3">
          <button mat-stroked-button type="button" class="h-10 rounded-xl!" (click)="loadCatalog()"><mat-icon>refresh</mat-icon>Actualizar</button>
          <span class="text-xs text-slate-500" aria-live="polite">{{ totalItems() }} registros encontrados</span>
        </div>

        @if (isLoading()) {
          <div class="space-y-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm" aria-label="Cargando catálogo" aria-busy="true">
            @for (row of loadingRows; track row) { <div class="h-10 animate-pulse rounded-lg bg-slate-100"></div> }
          </div>
        } @else if (error()) {
          <app-alert kind="error" title="No pudimos cargar el catálogo" [message]="error()!" actionLabel="Reintentar" (action)="loadCatalog()" />
        } @else {
          <div class="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-[0_4px_18px_rgb(15_23_42/4%)]">
            @if (items().length > 0) {
              <div class="overflow-x-auto">
                <table mat-table [dataSource]="items()" class="w-full min-w-190" [attr.aria-label]="'Catálogo de ' + selectedLabel()">
                  @if (selectedCatalog() === 'units') {
                    <ng-container matColumnDef="code"><th mat-header-cell *matHeaderCellDef>Clave</th><td mat-cell *matCellDef="let item">{{ item.code }}</td></ng-container>
                    <ng-container matColumnDef="name"><th mat-header-cell *matHeaderCellDef>Nombre</th><td mat-cell *matCellDef="let item">{{ item.name }}</td></ng-container>
                    <ng-container matColumnDef="alias"><th mat-header-cell *matHeaderCellDef>Alias</th><td mat-cell *matCellDef="let item">{{ item.alias }}</td></ng-container>
                  }
                  @if (selectedCatalog() === 'materialTypes') {
                    <ng-container matColumnDef="name"><th mat-header-cell *matHeaderCellDef>Tipo de material</th><td mat-cell *matCellDef="let item">{{ item.name }}</td></ng-container>
                  }
                  @if (selectedCatalog() === 'warehouses') {
                    <ng-container matColumnDef="id"><th mat-header-cell *matHeaderCellDef>Id</th><td mat-cell *matCellDef="let item">{{ item.id }}</td></ng-container>
                    <ng-container matColumnDef="code"><th mat-header-cell *matHeaderCellDef>Clave</th><td mat-cell *matCellDef="let item">{{ item.code }}</td></ng-container>
                    <ng-container matColumnDef="description"><th mat-header-cell *matHeaderCellDef>Descripción</th><td mat-cell *matCellDef="let item">{{ item.description }}</td></ng-container>
                  }
                  @if (selectedCatalog() === 'categories') {
                    <ng-container matColumnDef="code"><th mat-header-cell *matHeaderCellDef>Clave</th><td mat-cell *matCellDef="let item">{{ item.code }}</td></ng-container>
                    <ng-container matColumnDef="description"><th mat-header-cell *matHeaderCellDef>Descripción</th><td mat-cell *matCellDef="let item">{{ item.description }}</td></ng-container>
                    <ng-container matColumnDef="validDays"><th mat-header-cell *matHeaderCellDef>Días válidos</th><td mat-cell *matCellDef="let item">{{ item.validDays }}</td></ng-container>
                    <ng-container matColumnDef="months"><th mat-header-cell *matHeaderCellDef>Meses</th><td mat-cell *matCellDef="let item">{{ item.months }}</td></ng-container>
                  }
                  <tr mat-header-row *matHeaderRowDef="displayedColumns()"></tr>
                  <tr mat-row *matRowDef="let row; columns: displayedColumns()"></tr>
                </table>
              </div>
            } @else {
              <div class="p-4"><app-alert kind="info" title="No encontramos registros" message="Prueba con otra búsqueda o limpia los filtros." [actionLabel]="filter ? 'Limpiar búsqueda' : ''" (action)="clearFilter()" /></div>
            }
            <mat-paginator [length]="totalItems()" [pageSize]="pageSize" [pageSizeOptions]="[20, 50, 100]" (page)="changePage($event)" showFirstLastButtons [attr.aria-label]="'Paginación de ' + selectedLabel()"></mat-paginator>
          </div>
        }
      </main>
    </div>
  `,
})
export class AuxiliaryCatalogsPage implements OnInit {
  protected readonly catalogOptions: CatalogOption[] = [
    { key: 'units', label: 'Unidades', description: 'Unidades de medida' },
    { key: 'materialTypes', label: 'Tipos de material', description: 'Clasificación de materiales' },
    { key: 'warehouses', label: 'Almacenes', description: 'Almacenes operativos' },
    { key: 'categories', label: 'Categorías', description: 'Categorías de partida' },
  ];
  protected readonly loadingRows = [1, 2, 3, 4, 5];
  protected readonly selectedCatalog = signal<AuxiliaryCatalogKey>('units');
  protected readonly items = signal<AuxiliaryTableRow[]>([]);
  protected readonly totalItems = signal(0);
  protected readonly isLoading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly displayedColumns = computed(() => {
    switch (this.selectedCatalog()) {
      case 'units': return ['code', 'name', 'alias'];
      case 'materialTypes': return ['name'];
      case 'warehouses': return ['id', 'code', 'description'];
      case 'categories': return ['code', 'description', 'validDays', 'months'];
    }
  });
  protected filter = '';
  protected currentPage = 1;
  protected pageSize = 20;

  private requestSequence = 0;
  private readonly searchCatalog = inject(SearchAuxiliaryCatalogUseCase);
  private readonly notifications = inject(NotificationService);

  ngOnInit(): void {
    this.loadCatalog();
  }

  protected selectedLabel(): string {
    return this.catalogOptions.find((option) => option.key === this.selectedCatalog())?.label ?? 'catálogo';
  }

  protected selectCatalog(catalog: AuxiliaryCatalogKey): void {
    if (catalog === this.selectedCatalog()) return;
    this.selectedCatalog.set(catalog);
    this.currentPage = 1;
    this.filter = '';
    this.loadCatalog();
  }

  protected search(): void {
    this.currentPage = 1;
    this.loadCatalog();
  }

  protected clearFilter(): void {
    this.filter = '';
    this.search();
  }

  protected changePage(event: PageEvent): void {
    this.currentPage = event.pageIndex + 1;
    this.pageSize = event.pageSize;
    this.loadCatalog();
  }

  protected loadCatalog(): void {
    const requestId = ++this.requestSequence;
    this.isLoading.set(true);
    this.error.set(null);
    this.items.set([]);
    const criteria: AuxiliarySearchCriteria = { filter: this.filter, page: this.currentPage, pageSize: this.pageSize };
    this.query(criteria).subscribe({
      next: (response) => {
        if (requestId !== this.requestSequence) return;
        this.items.set(response.items);
        this.totalItems.set(response.total);
        this.isLoading.set(false);
      },
      error: (error: unknown) => {
        if (requestId !== this.requestSequence) return;
        this.isLoading.set(false);
        this.error.set(userFacingApiError(error, 'Verifica tu conexión e inténtalo nuevamente.'));
        this.notifications.error('No fue posible consultar el catálogo auxiliar.');
      },
    });
  }

  private query(criteria: AuxiliarySearchCriteria): Observable<AuxiliaryPage> {
    switch (this.selectedCatalog()) {
      case 'units': return this.searchCatalog.units(criteria);
      case 'materialTypes': return this.searchCatalog.materialTypes(criteria);
      case 'warehouses': return this.searchCatalog.warehouses(criteria);
      case 'categories': return this.searchCatalog.categories(criteria);
    }
  }
}
