import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { AppAlertComponent } from '@core/ui/app-alert/app-alert.component';
import { userFacingApiError } from '@core/http/api-error.util';
import { BusinessPartyKind, BusinessPartyRow } from '@features/catalogs/business-parties/domain/business-party.model';
import { BusinessPartyApi } from '@features/catalogs/business-parties/infrastructure/business-party.api';

/** Consulta agrupada de clientes, proveedores y agentes aduanales. */
@Component({
  selector: 'app-business-parties',
  imports: [AppAlertComponent, FormsModule, MatButtonModule, MatIconModule, MatInputModule, MatPaginatorModule, MatTableModule],
  template: `
    <div class="min-h-full bg-[#f4f7fb] text-slate-800"><main class="mx-auto w-full max-w-360 px-5 py-8 sm:px-8">
      <header class="mb-7"><p class="mb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Catálogos</p>
        <h1 class="m-0 text-2xl font-semibold text-slate-900">Socios comerciales</h1>
        <p class="mt-2 text-sm text-slate-500">Consulta read-only de maestros legacy confirmados.</p></header>
      <section class="mb-6 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm" aria-label="Seleccionar socio comercial">
        <div class="flex flex-wrap gap-2">@for (tab of tabs; track tab.kind) {
          <button mat-stroked-button type="button" [class.bg-blue-50]="selected()===tab.kind" [attr.aria-pressed]="selected()===tab.kind" (click)="select(tab.kind)">{{tab.label}}</button>
        }</div>
      </section>
      <section class="mb-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <form class="flex flex-wrap items-end gap-4" (submit)="search()"><label class="min-w-0 flex-1 sm:min-w-70"><span class="mb-1 block text-xs font-medium">Buscar</span>
          <input matInput name="filter" [(ngModel)]="filter" placeholder="Clave, nombre, RFC o patente" class="h-11 w-full rounded-xl border border-slate-200 px-4" /></label>
          <button mat-flat-button color="primary" type="submit">Consultar</button><button mat-stroked-button type="button" (click)="clear()">Limpiar</button></form>
      </section>
      @if (loading()) { <div class="rounded-2xl border border-slate-200 bg-white p-6" aria-busy="true">Cargando socios comerciales…</div> }
      @else if (error()) { <app-alert kind="error" title="No pudimos cargar los socios comerciales" [message]="error()!" actionLabel="Reintentar" (action)="load()" /> }
      @else { <section class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        @if (rows().length) { <div class="overflow-x-auto"><table mat-table [dataSource]="rows()" class="w-full min-w-190">
          <ng-container matColumnDef="key"><th mat-header-cell *matHeaderCellDef>Clave</th><td mat-cell *matCellDef="let row">{{row.key}}</td></ng-container>
          <ng-container matColumnDef="name"><th mat-header-cell *matHeaderCellDef>Nombre</th><td mat-cell *matCellDef="let row">{{row.name}}</td></ng-container>
          <ng-container matColumnDef="fiscalId"><th mat-header-cell *matHeaderCellDef>RFC / ID fiscal</th><td mat-cell *matCellDef="let row">{{row.fiscalId}}</td></ng-container>
          <ng-container matColumnDef="detail"><th mat-header-cell *matHeaderCellDef>{{ selected()==='agents' ? 'Patente / agencia' : 'País / correo' }}</th><td mat-cell *matCellDef="let row">{{ selected()==='agents' ? ((row.patent || '—') + ' · ' + (row.agency || '—')) : ((row.country || '—') + ' · ' + (row.email || '—')) }}</td></ng-container>
          <tr mat-header-row *matHeaderRowDef="columns"></tr><tr mat-row *matRowDef="let row; columns: columns"></tr>
        </table></div> } @else { <div class="p-5"><app-alert kind="info" title="Sin resultados" message="No hay registros para los filtros actuales." /></div> }
        <mat-paginator [length]="total()" [pageSize]="pageSize" [pageSizeOptions]="[20,50,100]" (page)="changePage($event)" showFirstLastButtons />
      </section> }
    </main></div>`,
})
export class BusinessPartiesPage implements OnInit {
  protected readonly tabs: { kind: BusinessPartyKind; label: string }[] = [{kind:'clients',label:'Clientes'},{kind:'providers',label:'Proveedores'},{kind:'agents',label:'Agentes aduanales'}];
  protected readonly selected=signal<BusinessPartyKind>('clients'); protected readonly rows=signal<BusinessPartyRow[]>([]); protected readonly total=signal(0);
  protected readonly loading=signal(false); protected readonly error=signal<string|null>(null); protected readonly columns=['key','name','fiscalId','detail'];
  protected filter=''; protected page=1; protected pageSize=20; private request=0; private readonly api=inject(BusinessPartyApi);
  ngOnInit(): void { this.load(); }
  protected select(kind: BusinessPartyKind): void { if(kind===this.selected()) return; this.selected.set(kind); this.filter=''; this.page=1; this.load(); }
  protected search(): void { this.page=1; this.load(); } protected clear(): void { this.filter=''; this.search(); }
  protected changePage(event: PageEvent): void { this.page=event.pageIndex+1; this.pageSize=event.pageSize; this.load(); }
  protected load(): void { const id=++this.request; this.loading.set(true); this.error.set(null); this.api.search(this.selected(),this.filter,this.page,this.pageSize).subscribe({next:r=>{if(id!==this.request)return;this.rows.set(r.items);this.total.set(r.total);this.loading.set(false);},error:e=>{if(id!==this.request)return;this.loading.set(false);this.error.set(userFacingApiError(e,'Verifica tu conexión e inténtalo nuevamente.'));}}); }
}
