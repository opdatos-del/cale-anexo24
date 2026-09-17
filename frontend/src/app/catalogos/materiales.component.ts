import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { MaterialDto, MaterialService } from './material.service';

/**
 * Consulta del catálogo de materiales (RF-010).
 */
@Component({
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatPaginatorModule, MatTableModule],
  selector: 'app-materiales',
  template: `
    <div class="header">
      <h2>Materiales</h2>
      <form (submit)="buscar()">
        <mat-form-field appearance="outline">
          <mat-label>Clave, descripción o fracción</mat-label>
          <input matInput name="filtro" [(ngModel)]="filtro" placeholder="ej. PINO" />
        </mat-form-field>
        <button mat-raised-button color="primary" type="submit">Buscar</button>
      </form>
    </div>

    @if (cargando()) {
      <p>Cargando…</p>
    } @else {
      <table mat-table [dataSource]="items()" class="mat-elevation-z2">
        <ng-container matColumnDef="clave">
          <th mat-header-cell *matHeaderCellDef>Clave</th>
          <td mat-cell *matCellDef="let m">{{ m.clave }}</td>
        </ng-container>
        <ng-container matColumnDef="descripcion">
          <th mat-header-cell *matHeaderCellDef>Descripción</th>
          <td mat-cell *matCellDef="let m">{{ m.descripcion }}</td>
        </ng-container>
        <ng-container matColumnDef="fraccion">
          <th mat-header-cell *matHeaderCellDef>Fracción</th>
          <td mat-cell *matCellDef="let m">{{ m.fraccion }}</td>
        </ng-container>
        <ng-container matColumnDef="unidad">
          <th mat-header-cell *matHeaderCellDef>Unidad</th>
          <td mat-cell *matCellDef="let m">{{ m.unidad }}</td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="columnas"></tr>
        <tr mat-row *matRowDef="let row; columns: columnas"></tr>
      </table>
      <mat-paginator
        [length]="total()"
        [pageSize]="tamano"
        [pageSizeOptions]="[5, 10, 20]"
        (page)="paginar($event)"
        showFirstLastButtons
      ></mat-paginator>
    }
  `,
  styles: [
    `
      .header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 16px;
        flex-wrap: wrap;
        padding-bottom: 12px;
      }
      form {
        display: flex;
        align-items: center;
        gap: 8px;
      }
      table {
        width: 100%;
      }
    `,
  ],
})
export class MaterialesComponent implements OnInit {
  protected columnas = ['clave', 'descripcion', 'fraccion', 'unidad'];
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

  protected paginar(event: PageEvent): void {
    this.pagina = event.pageIndex + 1;
    this.tamano = event.pageSize;
    this.cargar();
  }

  private cargar(): void {
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