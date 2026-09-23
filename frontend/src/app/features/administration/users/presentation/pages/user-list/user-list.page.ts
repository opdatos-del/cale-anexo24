import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ComponentType } from '@angular/cdk/portal';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { debounceTime, distinctUntilChanged, merge } from 'rxjs';
import { userFacingApiError } from '@core/http/api-error.util';
import { NotificationService } from '@core/notifications/notification.service';
import { ConfirmService } from '@core/ui/confirm-dialog/confirm.service';
import { ChangeUserStatusUseCase } from '../../../application/use-cases/change-user-status.use-case';
import { GetUserUseCase } from '../../../application/use-cases/get-user.use-case';
import { SearchUsersUseCase } from '../../../application/use-cases/search-users.use-case';
import { UserAdministration, UserStatus } from '../../../domain/models/user-administration.model';
import { UserEditDialog } from '../../dialogs/user-edit/user-edit.dialog';
import { UserExpirationDialog } from '../../dialogs/user-expiration/user-expiration.dialog';
import { UserPasswordDialog } from '../../dialogs/user-password/user-password.dialog';

/** Formatea una fecha civil sin conversiones de zona horaria. */
export function formatExpiration(value: string | null): string {
  if (!value) return 'Sin vencimiento';
  const [year, month, day] = value.split('-');
  return year && month && day ? `${day}/${month}/${year}` : value;
}

/** Administración paginada de usuarios. */
@Component({
  selector: 'app-user-list-page',
  imports: [
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
    ReactiveFormsModule,
  ],
  template: `
    <div class="min-h-full bg-[#f4f7fb] text-slate-800">
      <main class="mx-auto w-full max-w-360 px-4 py-8 sm:px-8">
        <header class="mb-6 flex flex-wrap items-end justify-between gap-3">
          <div>
            <p class="mb-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Administración</p>
            <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Usuarios</h1>
            <p class="mt-1 text-sm text-slate-500">Gestiona las cuentas con acceso al sistema.</p>
          </div>
          <div class="flex items-center gap-3">
            <span class="text-xs font-medium text-slate-500" aria-live="polite">{{ formatTotal() }} resultados</span>
            <button mat-stroked-button type="button" (click)="load()" [disabled]="loading()" aria-label="Actualizar listado de usuarios">
              <mat-icon aria-hidden="true">refresh</mat-icon> Actualizar
            </button>
          </div>
        </header>

        <section class="mb-4 rounded-2xl border border-slate-200/80 bg-white p-4 shadow-sm" aria-labelledby="user-filters">
          <h2 id="user-filters" class="sr-only">Filtros de usuarios</h2>
          <form [formGroup]="filters" class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>Nombre</mat-label><input matInput formControlName="name" autocomplete="off" />
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>Clave</mat-label><input matInput formControlName="key" autocomplete="off" />
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>Correo</mat-label><input matInput formControlName="email" autocomplete="off" />
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>Estado</mat-label>
              <mat-select formControlName="status">
                <mat-option [value]="null">Todos</mat-option>
                <mat-option value="ACTIVO">Activo</mat-option>
                <mat-option value="INACTIVO">Inactivo</mat-option>
              </mat-select>
            </mat-form-field>
          </form>
          <div class="mt-3 flex justify-end border-t border-slate-100 pt-3">
            <button mat-button type="button" (click)="clearFilters()">Limpiar filtros</button>
          </div>
        </section>

        <section class="overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-sm" aria-live="polite">
          @if (error()) {
            <div class="flex flex-col items-center gap-3 p-10 text-center" role="alert">
              <mat-icon class="text-red-600">error_outline</mat-icon>
              <p class="m-0 text-sm text-slate-700">{{ error() }}</p>
              <button mat-stroked-button type="button" (click)="load()">Reintentar</button>
            </div>
          } @else if (loading()) {
            <div class="flex items-center justify-center gap-3 p-12 text-sm text-slate-500">
              <mat-spinner diameter="28"></mat-spinner><span>Cargando usuarios…</span>
            </div>
          } @else if (users().length === 0) {
            <div class="p-12 text-center">
              <mat-icon class="text-slate-400">group_off</mat-icon>
              <p class="mb-1 mt-3 font-medium text-slate-700">{{ hasFilters() ? 'No se encontraron usuarios' : 'No hay usuarios para mostrar' }}</p>
              @if (hasFilters()) { <p class="m-0 text-sm text-slate-500">Prueba con otros filtros.</p> }
            </div>
          } @else {
            <div class="hidden overflow-x-auto md:block">
              <table mat-table [dataSource]="users()" class="w-full">
                <ng-container matColumnDef="user"><th mat-header-cell *matHeaderCellDef>Usuario</th><td mat-cell *matCellDef="let user"><div class="font-medium text-slate-900">{{ user.name }}</div><div class="text-xs text-slate-500">{{ user.key }}</div></td></ng-container>
                <ng-container matColumnDef="email"><th mat-header-cell *matHeaderCellDef>Correo</th><td mat-cell *matCellDef="let user">{{ user.email }}</td></ng-container>
                <ng-container matColumnDef="profile"><th mat-header-cell *matHeaderCellDef>Perfil</th><td mat-cell *matCellDef="let user">{{ user.profileName }}</td></ng-container>
                <ng-container matColumnDef="expiration"><th mat-header-cell *matHeaderCellDef>Vigencia</th><td mat-cell *matCellDef="let user">{{ formatExpiration(user.expiration) }}</td></ng-container>
                <ng-container matColumnDef="status"><th mat-header-cell *matHeaderCellDef>Estado</th><td mat-cell *matCellDef="let user"><span class="rounded-full px-2.5 py-1 text-xs font-semibold" [class]="user.status === 'ACTIVO' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'">{{ user.status === 'ACTIVO' ? 'Activo' : 'Inactivo' }}</span></td></ng-container>
                <ng-container matColumnDef="actions"><th mat-header-cell *matHeaderCellDef><span class="sr-only">Acciones</span></th><td mat-cell *matCellDef="let user" class="text-right"><button mat-icon-button type="button" [matMenuTriggerFor]="userActions" [matMenuTriggerData]="{ user: user }" [attr.aria-label]="'Acciones para ' + user.name"><mat-icon aria-hidden="true">more_vert</mat-icon></button></td></ng-container>
                <tr mat-header-row *matHeaderRowDef="columns"></tr><tr mat-row *matRowDef="let row; columns: columns"></tr>
              </table>
            </div>

            <mat-menu #userActions="matMenu">
              <ng-template matMenuContent let-user="user">
                <button mat-menu-item type="button" (click)="openEdit(user)"><mat-icon>edit</mat-icon><span>Editar datos</span></button>
                <button mat-menu-item type="button" (click)="confirmStatusChange(user)"><mat-icon>{{ user.status === 'ACTIVO' ? 'person_off' : 'person' }}</mat-icon><span>{{ user.status === 'ACTIVO' ? 'Inactivar usuario' : 'Activar usuario' }}</span></button>
                <button mat-menu-item type="button" (click)="openExpiration(user)"><mat-icon>event</mat-icon><span>Cambiar vigencia</span></button>
                <button mat-menu-item type="button" (click)="openPassword(user)"><mat-icon>key</mat-icon><span>Restablecer contraseña</span></button>
              </ng-template>
            </mat-menu>

            <div class="divide-y divide-slate-100 md:hidden">
              @for (user of users(); track user.id) {
                <article class="p-4">
                  <div class="flex items-start justify-between gap-3"><div><h3 class="m-0 font-semibold text-slate-900">{{ user.name }}</h3><p class="m-0 text-xs text-slate-500">{{ user.key }} · {{ user.profileName }}</p></div><span class="rounded-full px-2 py-1 text-xs font-semibold" [class]="user.status === 'ACTIVO' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'">{{ user.status === 'ACTIVO' ? 'Activo' : 'Inactivo' }}</span></div>
                  <dl class="my-3 grid gap-1 text-sm"><div><dt class="inline text-slate-500">Correo: </dt><dd class="inline break-all">{{ user.email }}</dd></div><div><dt class="inline text-slate-500">Vigencia: </dt><dd class="inline">{{ formatExpiration(user.expiration) }}</dd></div></dl>
                  <div class="flex flex-wrap gap-1"><button mat-button type="button" (click)="openEdit(user)">Editar</button><button mat-button type="button" (click)="openExpiration(user)">Vigencia</button><button mat-button type="button" (click)="openPassword(user)">Contraseña</button><button mat-button type="button" (click)="confirmStatusChange(user)">{{ user.status === 'ACTIVO' ? 'Inactivar' : 'Activar' }}</button></div>
                </article>
              }
            </div>
          }

          <mat-paginator [length]="total()" [pageIndex]="page() - 1" [pageSize]="pageSize()" [pageSizeOptions]="[10, 20, 50, 100]" showFirstLastButtons (page)="onPage($event)" aria-label="Paginación de usuarios"></mat-paginator>
        </section>
      </main>
    </div>
  `,
})
export class UserListPage implements OnInit {
  private readonly searchUsers = inject(SearchUsersUseCase);
  private readonly getUser = inject(GetUserUseCase);
  private readonly changeStatus = inject(ChangeUserStatusUseCase);
  private readonly notifications = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private requestSequence = 0;

  protected readonly columns = ['user', 'email', 'profile', 'expiration', 'status', 'actions'];
  protected readonly users = signal<UserAdministration[]>([]);
  protected readonly total = signal(0);
  protected readonly page = signal(1);
  protected readonly pageSize = signal(20);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly formatExpiration = formatExpiration;
  protected readonly filters = new FormGroup({
    name: new FormControl('', { nonNullable: true }),
    key: new FormControl('', { nonNullable: true }),
    email: new FormControl('', { nonNullable: true }),
    status: new FormControl<UserStatus | null>(null),
  });

  ngOnInit(): void {
    merge(this.filters.controls.name.valueChanges, this.filters.controls.key.valueChanges, this.filters.controls.email.valueChanges)
      .pipe(debounceTime(500), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.filtersChanged());
    this.filters.controls.status.valueChanges.pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef)).subscribe(() => this.filtersChanged());
    this.load();
  }

  protected load(): void {
    const requestId = ++this.requestSequence;
    this.loading.set(true);
    this.error.set(null);
    const values = this.filters.getRawValue();
    this.searchUsers.execute({ ...values, page: this.page(), pageSize: this.pageSize() }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (result) => {
        if (requestId !== this.requestSequence) return;
        this.users.set(result.items);
        this.total.set(result.total);
        this.page.set(result.page);
        this.pageSize.set(result.pageSize);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        if (requestId !== this.requestSequence) return;
        this.loading.set(false);
        this.error.set(userFacingApiError(error, 'No fue posible consultar los usuarios.'));
        this.notifications.error('No fue posible consultar los usuarios.');
      },
    });
  }

  protected formatTotal(): string {
    return new Intl.NumberFormat('es-MX').format(this.total());
  }

  protected hasFilters(): boolean {
    const values = this.filters.getRawValue();
    return Boolean(values.name.trim() || values.key.trim() || values.email.trim() || values.status);
  }

  protected clearFilters(): void {
    this.filters.reset({ name: '', key: '', email: '', status: null }, { emitEvent: false });
    this.page.set(1);
    this.load();
  }

  protected onPage(event: PageEvent): void {
    this.page.set(event.pageIndex + 1);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  protected openEdit(user: UserAdministration): void {
    this.openDetailDialog(user.id, UserEditDialog);
  }

  protected openExpiration(user: UserAdministration): void {
    this.openDetailDialog(user.id, UserExpirationDialog);
  }

  protected openPassword(user: UserAdministration): void {
    this.openDetailDialog(user.id, UserPasswordDialog, false);
  }

  protected confirmStatusChange(user: UserAdministration): void {
    const status: UserStatus = user.status === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    const action = status === 'ACTIVO' ? 'activar' : 'inactivar';
    const message = status === 'INACTIVO'
      ? `¿Deseas inactivar a ${user.name}? Ya no podrá iniciar sesión.`
      : `¿Deseas activar a ${user.name}?`;
    this.confirm.ask({ title: `${action === 'activar' ? 'Activar' : 'Inactivar'} usuario`, message, confirmLabel: action === 'activar' ? 'Activar' : 'Inactivar', kind: status === 'INACTIVO' ? 'danger' : 'warning' }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((confirmed) => {
      if (!confirmed) return;
      this.changeStatus.execute(user.id, { status }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => {
          this.notifications.success(`Usuario ${status === 'ACTIVO' ? 'activado' : 'inactivado'}.`);
          this.load();
        },
        error: (error: unknown) => this.notifications.error(userFacingApiError(error, 'No fue posible cambiar el estado.')),
      });
    });
  }

  private filtersChanged(): void {
    this.page.set(1);
    this.load();
  }

  private openDetailDialog<T>(id: number, component: ComponentType<T>, reload = true): void {
    this.getUser.execute(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (detail) => {
        this.dialog.open(component, { data: detail, width: 'min(94vw, 560px)', maxWidth: 'calc(100vw - 24px)', autoFocus: 'first-tabbable', restoreFocus: true }).afterClosed().subscribe((result: unknown) => {
          if (result && reload) this.load();
        });
      },
      error: (error: unknown) => this.notifications.error(userFacingApiError(error, 'No fue posible obtener el detalle del usuario.')),
    });
  }
}
