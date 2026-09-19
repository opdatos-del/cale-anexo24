import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { EMPTY, Subject, catchError, debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs';
import { NotificationService } from '../../../../../../core/notifications/notification.service';
import { AppAlertComponent } from '../../../../../../core/ui/app-alert/app-alert.component';
import { AppPanelComponent } from '../../../../../../core/ui/app-panel/app-panel.component';
import { EmptyStateComponent } from '../../../../../../core/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../../../../core/ui/page-header/page-header.component';
import { StatusBadgeComponent } from '../../../../../../core/ui/status-badge/status-badge.component';
import { SearchMaterialsUseCase } from '../../../application/use-cases/search-materials.use-case';
import { Material } from '../../../domain/models/material.model';
import { MaterialPage, MaterialSearchCriteria } from '../../../domain/repositories/material.repository';

/** Consulta incremental del catálogo de materiales (RF-010). */
@Component({
  imports: [
    AppAlertComponent,
    AppPanelComponent,
    EmptyStateComponent,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatTableModule,
    PageHeaderComponent,
    ReactiveFormsModule,
    StatusBadgeComponent,
  ],
  selector: 'app-materiales',
  styleUrl: './material-list.page.scss',
  template: `
    <div class="app-page">
      <main class="app-page__content">
        <app-page-header
          eyebrow="Catálogos"
          title="Materiales"
          description="Consulta el catálogo de materiales registrado en Módulo C."
        />

        <app-panel class="mb-6" aria-labelledby="filtros-materiales">
          <h2 id="filtros-materiales" class="sr-only">Filtros de materiales</h2>
          <div class="catalog-filter-form">
            <label class="catalog-filter-label">
              <span>Parte, descripción o fracción</span>
              <span class="catalog-filter-control">
                <mat-icon aria-hidden="true">search</mat-icon>
                <input
                  matInput
                  name="filter"
                  [formControl]="filterControl"
                  placeholder="Escribe para buscar"
                  autocomplete="off"
                  aria-describedby="filtro-ayuda"
                />
                @if (filterValue()) {
                  <button type="button" class="catalog-filter-clear" aria-label="Limpiar búsqueda" (click)="clearFilter()">
                    <mat-icon aria-hidden="true">close</mat-icon>
                  </button>
                }
              </span>
              <small id="filtro-ayuda">Los resultados se actualizan mientras escribes.</small>
            </label>
          </div>
        </app-panel>

        <div class="catalog-results-toolbar">
          <div class="catalog-results-summary">
            <span class="catalog-results-label">Resultados</span>
            <app-status-badge kind="neutral" [label]="totalItems() + ' registros'" />
            @if (isLoading()) {
              <span class="catalog-searching" aria-live="polite">Buscando...</span>
            }
          </div>
          <button mat-stroked-button type="button" class="h-10 rounded-xl!" [disabled]="isLoading()" (click)="loadMaterials()">
            <mat-icon aria-hidden="true">refresh</mat-icon>
            Actualizar
          </button>
        </div>

        @if (isLoading()) {
          <div class="catalog-loading-panel" aria-label="Cargando materiales" aria-busy="true">
            <div class="catalog-loading-header"></div>
            @for (row of loadingRows; track row) {
              <div class="catalog-loading-row">
                <span></span><span></span><span></span><span></span><span></span><span></span>
              </div>
            }
          </div>
        } @else if (error()) {
          <app-alert kind="error" title="No pudimos cargar los materiales" [message]="error()!" actionLabel="Reintentar" (action)="loadMaterials()" />
        } @else {
          <div class="catalog-table-panel">
            @if (items().length > 0) {
              <div class="overflow-x-auto">
                <table mat-table [dataSource]="items()" class="w-full min-w-190" aria-label="Catálogo de materiales">
                  <ng-container matColumnDef="partNumber">
                    <th mat-header-cell *matHeaderCellDef class="w-40">N° parte</th>
                    <td mat-cell *matCellDef="let m" class="font-medium text-slate-700">{{ m.partNumber }}</td>
                  </ng-container>
                  <ng-container matColumnDef="description">
                    <th mat-header-cell *matHeaderCellDef>Descripción</th>
                    <td mat-cell *matCellDef="let m" class="max-w-140">{{ m.description }}</td>
                  </ng-container>
                  <ng-container matColumnDef="tariffFraction">
                    <th mat-header-cell *matHeaderCellDef class="w-36">Fracción</th>
                    <td mat-cell *matCellDef="let m">{{ m.tariffFraction }}</td>
                  </ng-container>
                  <ng-container matColumnDef="unit">
                    <th mat-header-cell *matHeaderCellDef class="w-28">Unidad</th>
                    <td mat-cell *matCellDef="let m">{{ m.unit }}</td>
                  </ng-container>
                  <ng-container matColumnDef="umc">
                    <th mat-header-cell *matHeaderCellDef class="w-28">UMC</th>
                    <td mat-cell *matCellDef="let m">{{ m.umc }}</td>
                  </ng-container>
                  <ng-container matColumnDef="materialType">
                    <th mat-header-cell *matHeaderCellDef class="w-40">Tipo</th>
                    <td mat-cell *matCellDef="let m">{{ m.materialType }}</td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
                </table>
              </div>
            } @else {
              <app-empty-state
                icon="inventory_2"
                title="No encontramos materiales"
                message="Prueba con otra búsqueda o limpia el campo para consultar nuevamente."
                [actionLabel]="filterValue() ? 'Limpiar búsqueda' : ''"
                (action)="clearFilter()"
              />
            }
            <mat-paginator
              [length]="totalItems()"
              [pageIndex]="currentPage - 1"
              [pageSize]="pageSize"
              [pageSizeOptions]="[5, 10, 20]"
              (page)="changePage($event)"
              showFirstLastButtons
              aria-label="Paginación de materiales"
            ></mat-paginator>
          </div>
        }
      </main>
    </div>
  `,
})
export class MaterialListPage implements OnInit {
  protected readonly displayedColumns = ['partNumber', 'description', 'tariffFraction', 'unit', 'umc', 'materialType'];
  protected readonly items = signal<Material[]>([]);
  protected readonly totalItems = signal(0);
  protected readonly isLoading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly filterValue = signal('');
  protected readonly loadingRows = [1, 2, 3, 4, 5];
  protected readonly filterControl = new FormControl('', { nonNullable: true });
  protected currentPage = 1;
  protected pageSize = 10;

  private readonly searchMaterials = inject(SearchMaterialsUseCase);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchRequests = new Subject<MaterialSearchCriteria>();

  ngOnInit(): void {
    this.searchRequests
      .pipe(
        tap(() => {
          this.isLoading.set(true);
          this.error.set(null);
        }),
        switchMap((criteria) =>
          this.searchMaterials.execute(criteria).pipe(
            catchError(() => {
              this.isLoading.set(false);
              this.error.set('Verifica tu conexión e inténtalo nuevamente.');
              this.notifications.error('No fue posible consultar el catálogo de materiales.');
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((response) => this.applyResponse(response));

    this.filterControl.valueChanges
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => {
        this.filterValue.set(value);
        this.currentPage = 1;
        this.requestSearch(1, this.pageSize, value);
      });

    this.requestSearch(1, this.pageSize, this.filterControl.value);
  }

  protected clearFilter(): void {
    if (!this.filterControl.value) return;
    this.filterControl.setValue('');
  }

  protected changePage(event: PageEvent): void {
    this.currentPage = event.pageIndex + 1;
    this.pageSize = event.pageSize;
    this.requestSearch(this.currentPage, this.pageSize, this.filterControl.value);
  }

  protected loadMaterials(): void {
    this.requestSearch(this.currentPage, this.pageSize, this.filterControl.value);
  }

  private requestSearch(page: number, pageSize: number, filter: string): void {
    this.searchRequests.next({ filter, page, pageSize });
  }

  private applyResponse(response: MaterialPage): void {
    this.items.set(response.items);
    this.totalItems.set(response.total);
    this.isLoading.set(false);
  }
}
