import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { MaterialDto, MaterialService } from './material.service';

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
          <form (submit)="buscar()" class="flex flex-wrap items-end gap-4">
            <label class="min-w-0 flex-1 sm:min-w-[280px]">
              <span class="mb-1 block text-xs font-medium text-slate-700">Parte, descripción o fracción</span>
              <input
                matInput
                name="filtro"
                [(ngModel)]="filtro"
                placeholder="Buscar en el catálogo"
                class="h-10 w-full rounded-lg border border-slate-400 bg-white px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-600 focus:ring-2 focus:ring-blue-600/15"
              />
            </label>
            <button mat-flat-button color="primary" type="submit" class="h-10 min-w-28">Consultar</button>
            <button mat-stroked-button type="button" class="h-10 min-w-24" (click)="limpiar()">Limpiar</button>
          </form>
        </section>

        <div class="mb-3 flex flex-wrap items-center gap-3">
          <button mat-flat-button color="primary" type="button" class="h-10">+ Nuevo</button>
          <button mat-stroked-button type="button" class="h-10">⇩ Exportar</button>
          <button mat-stroked-button type="button" class="h-10" (click)="cargar()">⟳ Refrescar</button>
        </div>

        @if (cargando()) {
          <div class="rounded border border-slate-200 bg-white px-4 py-8 text-center text-sm text-slate-500">Cargando…</div>
        } @else {
          <div class="overflow-hidden rounded border border-slate-200 bg-white shadow-sm">
            <div class="overflow-x-auto">
              <table mat-table [dataSource]="items()" class="w-full min-w-[760px]">
                <ng-container matColumnDef="clave">
                  <th mat-header-cell *matHeaderCellDef class="w-40">N° parte</th>
                  <td mat-cell *matCellDef="let m" class="font-medium text-slate-700">{{ m.clave }}</td>
                </ng-container>
                <ng-container matColumnDef="descripcion">
                  <th mat-header-cell *matHeaderCellDef>Descripción</th>
                  <td mat-cell *matCellDef="let m" class="max-w-[560px]">{{ m.descripcion }}</td>
                </ng-container>
                <ng-container matColumnDef="fraccion">
                  <th mat-header-cell *matHeaderCellDef class="w-36">Fracción</th>
                  <td mat-cell *matCellDef="let m">{{ m.fraccion }}</td>
                </ng-container>
                <ng-container matColumnDef="unidad">
                  <th mat-header-cell *matHeaderCellDef class="w-28">Unidad</th>
                  <td mat-cell *matCellDef="let m">{{ m.unidad }}</td>
                </ng-container>
                <ng-container matColumnDef="unidadt">
                  <th mat-header-cell *matHeaderCellDef class="w-28">UMC</th>
                  <td mat-cell *matCellDef="let m">{{ m.unidadt }}</td>
                </ng-container>
                <ng-container matColumnDef="tipo">
                  <th mat-header-cell *matHeaderCellDef class="w-40">Tipo</th>
                  <td mat-cell *matCellDef="let m">{{ m.tipomaterial }}</td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="columnas"></tr>
                <tr mat-row *matRowDef="let row; columns: columnas"></tr>
              </table>
            </div>
            @if (items().length === 0) {
              <div class="border-t border-slate-200 px-4 py-8 text-center text-sm text-slate-500">No se encontraron registros.</div>
            }
            <mat-paginator
              [length]="total()"
              [pageSize]="tamano"
              [pageSizeOptions]="[5, 10, 20]"
              (page)="paginar($event)"
              showFirstLastButtons
            ></mat-paginator>
          </div>
        }
      </main>
    </div>
  `,
})
export class MaterialesComponent implements OnInit {
  protected columnas = ['clave', 'descripcion', 'fraccion', 'unidad', 'unidadt', 'tipo'];
  protected items = signal<MaterialDto[]>([]);
  protected total = signal(0);
  protected cargando = signal(false);
  protected filtro = '';
  protected pagina = 1;
  protected tamano = 10;

  private readonly service = inject(MaterialService);

  ngOnInit(): void {
    this.cargar();
  }

  protected buscar(): void {
    this.pagina = 1;
    this.cargar();
  }

  protected limpiar(): void {
    this.filtro = '';
    this.buscar();
  }

  protected paginar(event: PageEvent): void {
    this.pagina = event.pageIndex + 1;
    this.tamano = event.pageSize;
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.service.listar(this.filtro, this.pagina, this.tamano).subscribe({
      next: (resp) => {
        this.items.set(resp.items);
        this.total.set(resp.total);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
