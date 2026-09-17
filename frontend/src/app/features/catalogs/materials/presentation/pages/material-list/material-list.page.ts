import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { Material } from '../../../domain/models/material.model';
import { SearchMaterialsUseCase } from '../../../application/use-cases/search-materials.use-case';

/**
 * Consulta del catálogo de materiales (RF-010).
 */
@Component({
  imports: [FormsModule, MatButtonModule, MatInputModule, MatPaginatorModule, MatTableModule],
  selector: 'app-materiales',
  template: `
    <div class="min-h-full bg-[#f4f7fb] text-slate-800">
      <main class="mx-auto w-full max-w-[1440px] px-5 py-8 sm:px-8">
        <div class="mb-7 flex flex-wrap items-end justify-between gap-3">
          <div>
            <p class="mb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Catálogos</p>
            <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Materiales</h1>
            <p class="mt-2 text-sm text-slate-500">Consulta y administra el catálogo de materiales.</p>
          </div>
        </div>

        <section class="mb-6 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-[0_4px_18px_rgb(15_23_42_/_4%)]" aria-labelledby="filtros-materiales">
          <h2 id="filtros-materiales" class="sr-only">Filtros de materiales</h2>
          <form (submit)="search()" class="flex flex-wrap items-end gap-4">
            <label class="min-w-0 flex-1 sm:min-w-[280px]">
              <span class="mb-1 block text-xs font-medium text-slate-700">Parte, descripción o fracción</span>
              <input
                matInput
                name="filter"
                [(ngModel)]="filter"
                placeholder="Buscar en el catálogo"
                class="h-11 w-full rounded-xl border border-slate-200 bg-slate-50 px-4 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10"
              />
            </label>
            <button mat-flat-button color="primary" type="submit" class="h-11 min-w-28 !rounded-xl">Consultar</button>
            <button mat-stroked-button type="button" class="h-11 min-w-24 !rounded-xl" (click)="clearFilter()">Limpiar</button>
          </form>
        </section>

        <div class="mb-4 flex flex-wrap items-center gap-3">
          <button mat-flat-button color="primary" type="button" class="h-10 !rounded-xl">+ Nuevo</button>
          <button mat-stroked-button type="button" class="h-10 !rounded-xl">⇩ Exportar</button>
          <button mat-stroked-button type="button" class="h-10 !rounded-xl" (click)="loadMaterials()">⟳ Refrescar</button>
        </div>

        @if (isLoading()) {
          <div class="rounded-2xl border border-slate-200 bg-white px-4 py-10 text-center text-sm text-slate-500 shadow-sm">Cargando…</div>
        } @else {
          <div class="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-[0_4px_18px_rgb(15_23_42_/_4%)]">
            <div class="overflow-x-auto">
              <table mat-table [dataSource]="items()" class="w-full min-w-[760px]">
                <ng-container matColumnDef="partNumber">
                  <th mat-header-cell *matHeaderCellDef class="w-40">N° parte</th>
                  <td mat-cell *matCellDef="let m" class="font-medium text-slate-700">{{ m.partNumber }}</td>
                </ng-container>
                <ng-container matColumnDef="description">
                  <th mat-header-cell *matHeaderCellDef>Descripción</th>
                  <td mat-cell *matCellDef="let m" class="max-w-[560px]">{{ m.description }}</td>
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
            @if (items().length === 0) {
              <div class="border-t border-slate-200 px-4 py-8 text-center text-sm text-slate-500">No se encontraron registros.</div>
            }
            <mat-paginator
              [length]="totalItems()"
              [pageSize]="pageSize"
              [pageSizeOptions]="[5, 10, 20]"
              (page)="changePage($event)"
              showFirstLastButtons
            ></mat-paginator>
          </div>
        }
      </main>
    </div>
  `,
})
export class MaterialListPage implements OnInit {
  protected displayedColumns = ['partNumber', 'description', 'tariffFraction', 'unit', 'umc', 'materialType'];
  protected items = signal<Material[]>([]);
  protected totalItems = signal(0);
  protected isLoading = signal(false);
  protected filter = '';
  protected currentPage = 1;
  protected pageSize = 10;

  private readonly searchMaterials = inject(SearchMaterialsUseCase);

  ngOnInit(): void {
    this.loadMaterials();
  }

  protected search(): void {
    this.currentPage = 1;
    this.loadMaterials();
  }

  protected clearFilter(): void {
    this.filter = '';
    this.search();
  }

  protected changePage(event: PageEvent): void {
    this.currentPage = event.pageIndex + 1;
    this.pageSize = event.pageSize;
    this.loadMaterials();
  }

  protected loadMaterials(): void {
    this.isLoading.set(true);
    this.searchMaterials.execute({ filter: this.filter, page: this.currentPage, pageSize: this.pageSize }).subscribe({
      next: (resp) => {
        this.items.set(resp.items);
        this.totalItems.set(resp.total);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }
}
