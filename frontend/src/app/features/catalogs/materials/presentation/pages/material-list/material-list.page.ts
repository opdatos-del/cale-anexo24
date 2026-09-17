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
    <div class="min-h-screen bg-slate-50 text-slate-800">
      <header class="flex h-14 items-center justify-between bg-blue-700 px-6 text-white shadow-sm">
        <span class="text-sm font-semibold tracking-wide">CALE · ANEXO 24</span>
        <span class="text-xs font-medium uppercase tracking-[0.16em] text-blue-100">Catálogo · Materiales</span>
      </header>

      <main class="mx-auto w-full max-w-[1440px] px-6 py-6">
        <div class="mb-5">
          <h1 class="m-0 text-2xl font-medium tracking-tight text-slate-800">Catálogo · Materiales</h1>
        </div>

        <section class="mb-5 rounded-sm bg-slate-100 p-4" aria-labelledby="filtros-materiales">
          <h2 id="filtros-materiales" class="sr-only">Filtros de materiales</h2>
          <form (submit)="search()" class="flex flex-wrap items-end gap-4">
            <label class="min-w-0 flex-1 sm:min-w-[280px]">
              <span class="mb-1 block text-xs font-medium text-slate-700">Parte, descripción o fracción</span>
              <input
                matInput
                name="filter"
                [(ngModel)]="filter"
                placeholder="Buscar en el catálogo"
                class="h-10 w-full rounded-lg border border-slate-400 bg-white px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-600 focus:ring-2 focus:ring-blue-600/15"
              />
            </label>
            <button mat-flat-button color="primary" type="submit" class="h-10 min-w-28">Consultar</button>
              <button mat-stroked-button type="button" class="h-10 min-w-24" (click)="clearFilter()">Limpiar</button>
          </form>
        </section>

        <div class="mb-3 flex flex-wrap items-center gap-3">
          <button mat-flat-button color="primary" type="button" class="h-10">+ Nuevo</button>
          <button mat-stroked-button type="button" class="h-10">⇩ Exportar</button>
          <button mat-stroked-button type="button" class="h-10" (click)="loadMaterials()">⟳ Refrescar</button>
        </div>

        @if (isLoading()) {
          <div class="rounded border border-slate-200 bg-white px-4 py-8 text-center text-sm text-slate-500">Cargando…</div>
        } @else {
          <div class="overflow-hidden rounded border border-slate-200 bg-white shadow-sm">
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
