import { Component, DestroyRef, OnInit, ViewContainerRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { AuthService } from '@core/auth/auth.service';
import { userFacingApiError } from '@core/http/api-error.util';
import { NotificationService } from '@core/notifications/notification.service';
import { AppAlertComponent } from '@core/ui/app-alert/app-alert.component';
import { ConfirmService } from '@core/ui/confirm-dialog/confirm.service';
import { ChangeProfileStatusUseCase } from '../../../application/use-cases/change-profile-status.use-case';
import { CreateProfileUseCase } from '../../../application/use-cases/create-profile.use-case';
import { GetProfilePermissionsUseCase } from '../../../application/use-cases/get-profile-permissions.use-case';
import { ListProfileActivitiesUseCase } from '../../../application/use-cases/list-profile-activities.use-case';
import { LoadAllProfilesUseCase } from '../../../application/use-cases/load-all-profiles.use-case';
import { ReplaceProfilePermissionsUseCase } from '../../../application/use-cases/replace-profile-permissions.use-case';
import { UpdateProfileNameUseCase } from '../../../application/use-cases/update-profile-name.use-case';
import { ProfileActivity, ProfilePermissions } from '../../../domain/models/profile-permission.model';
import { ProfileAdministration, ProfileStatus } from '../../../domain/models/profile-administration.model';
import { ProfileCreateDialog } from '../../dialogs/profile-create/profile-create.dialog';

/** Administración de perfiles, estado y conjunto final de permisos. */
@Component({
  selector: 'app-profile-management-page',
  imports: [
    AppAlertComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    ReactiveFormsModule,
  ],
  template: `
    <div class="min-h-full bg-[#f4f7fb] text-slate-800">
      <main class="mx-auto w-full max-w-360 px-4 py-7 sm:px-8">
        <header class="mb-6 flex flex-wrap items-end justify-between gap-4">
          <div>
            <p class="mb-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-600">Administración</p>
            <h1 class="m-0 text-2xl font-semibold tracking-tight text-slate-900">Perfiles</h1>
            <p class="mt-1 text-sm text-slate-500">Define qué puede consultar y administrar cada perfil.</p>
          </div>
          <button mat-flat-button type="button" (click)="openCreate()" [disabled]="!canManage()" aria-label="Crear perfil">
            <mat-icon aria-hidden="true">add</mat-icon> Nuevo perfil
          </button>
        </header>

        @if (!canManage()) {
          <app-alert kind="warning" title="Acceso de consulta" message="Tu sesión puede consultar perfiles, pero necesitas PERFILES_ADMINISTRAR para modificar perfiles y permisos." />
        }

        <div class="grid min-w-0 gap-4 xl:grid-cols-[minmax(250px,0.72fr)_minmax(0,1.8fr)]">
          <section class="min-w-0 overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-sm" aria-labelledby="profiles-heading">
            <div class="border-b border-slate-100 p-4">
              <div class="mb-3 flex items-center justify-between gap-3">
                <h2 id="profiles-heading" class="m-0 text-sm font-semibold text-slate-800">Perfiles disponibles</h2>
                <button mat-icon-button type="button" (click)="loadProfiles()" [disabled]="profilesLoading()" aria-label="Actualizar perfiles"><mat-icon>refresh</mat-icon></button>
              </div>
              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="w-full">
                <mat-label>Buscar perfil</mat-label><input matInput [formControl]="profileSearch" autocomplete="off" />
                <mat-icon matIconPrefix aria-hidden="true">search</mat-icon>
              </mat-form-field>
            </div>
            @if (profilesError()) {
              <div class="p-4"><app-alert kind="error" title="No pudimos cargar los perfiles" [message]="profilesError()!" actionLabel="Reintentar" (action)="loadProfiles()" /></div>
            } @else if (profilesLoading()) {
              <div class="flex items-center justify-center gap-3 p-8 text-sm text-slate-500" aria-busy="true"><mat-spinner diameter="24" /><span>Cargando perfiles…</span></div>
            } @else if (filteredProfiles().length === 0) {
              <div class="p-8 text-center" role="status">
                <mat-icon class="text-slate-400" aria-hidden="true">badge</mat-icon>
                <p class="mb-1 mt-2 text-sm font-medium text-slate-700">{{ profiles().length ? 'No hay coincidencias' : 'Todavía no hay perfiles' }}</p>
                @if (!profiles().length && canManage()) { <p class="m-0 text-xs text-slate-500">Crea el primero para definir sus permisos.</p> }
              </div>
            } @else {
              <ul class="m-0 list-none divide-y divide-slate-100 p-0" aria-label="Lista de perfiles">
                @for (profile of filteredProfiles(); track profile.id) {
                  <li>
                    <button type="button" class="flex w-full items-start justify-between gap-3 px-4 py-4 text-left transition hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-blue-600" [class.bg-blue-50]="selectedProfile()?.id === profile.id" [attr.aria-pressed]="selectedProfile()?.id === profile.id" [disabled]="commandBusy()" (click)="selectProfile(profile)">
                      <span class="min-w-0"><span class="block truncate text-sm font-semibold text-slate-800">{{ profile.name }}</span><span class="mt-1 block text-xs text-slate-500">{{ profile.permissionCount }} permisos asignados</span></span>
                      <span class="shrink-0 rounded-full border px-2 py-1 text-[11px] font-semibold" [class.border-emerald-200]="profile.status === 'ACTIVO'" [class.bg-emerald-50]="profile.status === 'ACTIVO'" [class.text-emerald-800]="profile.status === 'ACTIVO'" [class.border-slate-200]="profile.status === 'INACTIVO'" [class.bg-slate-100]="profile.status === 'INACTIVO'" [class.text-slate-700]="profile.status === 'INACTIVO'">{{ profile.status === 'ACTIVO' ? 'Activo' : 'Inactivo' }}</span>
                    </button>
                  </li>
                }
              </ul>
            }
          </section>

          <section class="min-w-0 rounded-2xl border border-slate-200/80 bg-white shadow-sm" aria-labelledby="profile-detail-heading">
            @if (!selectedProfile()) {
              <div class="flex min-h-72 flex-col items-center justify-center p-8 text-center" role="status">
                <span class="mb-3 grid size-12 place-items-center rounded-2xl bg-blue-50 text-blue-700"><mat-icon aria-hidden="true">tune</mat-icon></span>
                <h2 id="profile-detail-heading" class="m-0 text-base font-semibold text-slate-800">Selecciona un perfil</h2>
                <p class="mt-2 max-w-sm text-sm text-slate-500">Su información y permisos aparecerán aquí para consulta o edición.</p>
              </div>
            } @else {
              <div class="flex flex-wrap items-start justify-between gap-4 border-b border-slate-100 p-5 sm:p-6">
                <div class="min-w-0 flex-1">
                  <p class="mb-1 text-[10px] font-semibold uppercase tracking-[0.16em] text-slate-400">Perfil seleccionado</p>
                  <h2 id="profile-detail-heading" class="m-0 break-words text-xl font-semibold tracking-tight text-slate-900">{{ selectedProfile()!.name }}</h2>
                  <span class="mt-2 inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold" [class.border-emerald-200]="selectedProfile()!.status === 'ACTIVO'" [class.bg-emerald-50]="selectedProfile()!.status === 'ACTIVO'" [class.text-emerald-800]="selectedProfile()!.status === 'ACTIVO'" [class.border-slate-200]="selectedProfile()!.status === 'INACTIVO'" [class.bg-slate-100]="selectedProfile()!.status === 'INACTIVO'" [class.text-slate-700]="selectedProfile()!.status === 'INACTIVO'">{{ selectedProfile()!.status === 'ACTIVO' ? 'Activo' : 'Inactivo' }}</span>
                </div>
                <div class="flex flex-wrap gap-2">
                  @if (canManage()) {
                    <button mat-stroked-button type="button" (click)="toggleStatus()" [disabled]="commandBusy()" [attr.aria-label]="selectedProfile()!.status === 'ACTIVO' ? 'Inactivar perfil' : 'Activar perfil'">
                      <mat-icon aria-hidden="true">{{ selectedProfile()!.status === 'ACTIVO' ? 'pause_circle' : 'play_circle' }}</mat-icon>
                      {{ selectedProfile()!.status === 'ACTIVO' ? 'Inactivar' : 'Activar' }}
                    </button>
                  }
                </div>
              </div>

              <div class="grid gap-6 p-5 sm:p-6">
                <form class="grid gap-2 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-start" (ngSubmit)="saveName()" aria-label="Cambiar nombre del perfil">
                  <mat-form-field appearance="outline" subscriptSizing="dynamic">
                    <mat-label>Nombre del perfil</mat-label>
                    <input matInput [formControl]="profileName" maxlength="80" autocomplete="off" [readonly]="!canManage()" />
                    @if (profileName.touched && profileName.invalid) { <mat-error>Ingresa nombre de hasta 80 caracteres.</mat-error> }
                  </mat-form-field>
                  @if (canManage()) { <button mat-stroked-button type="submit" class="sm:mt-0.5" [disabled]="commandBusy() || !nameChanged()">Guardar nombre</button> }
                </form>

                <div class="border-t border-slate-100 pt-5">
                  <div class="mb-4 flex flex-wrap items-end justify-between gap-3">
                    <div>
                      <h3 class="m-0 text-base font-semibold text-slate-900">Permisos</h3>
                      <p class="mt-1 mb-0 text-sm text-slate-500">Selecciona el conjunto final de actividades para este perfil.</p>
                    </div>
                    <span class="rounded-lg bg-slate-100 px-3 py-2 text-xs font-semibold text-slate-700" aria-live="polite">{{ selectedIds().length }} seleccionadas</span>
                  </div>

                  @if (permissionsError()) {
                    <app-alert kind="error" title="No pudimos cargar permisos" [message]="permissionsError()!" actionLabel="Reintentar" (action)="loadSelectedPermissions()" />
                  } @else if (permissionsLoading() || activitiesLoading()) {
                    <div class="flex items-center gap-3 rounded-xl bg-slate-50 p-5 text-sm text-slate-500" aria-busy="true"><mat-spinner diameter="22" /><span>Cargando catálogo y permisos…</span></div>
                  } @else if (activitiesError()) {
                    <app-alert kind="error" title="No pudimos cargar actividades" [message]="activitiesError()!" actionLabel="Reintentar" (action)="loadActivities()" />
                  } @else {
                    <mat-form-field appearance="outline" subscriptSizing="dynamic" class="mb-3 w-full">
                      <mat-label>Buscar actividad</mat-label><input matInput [formControl]="activitySearch" autocomplete="off" />
                      <mat-icon matIconPrefix aria-hidden="true">search</mat-icon>
                    </mat-form-field>
                    @if (filteredActivities().length === 0) {
                      <div class="rounded-xl border border-dashed border-slate-200 p-6 text-center text-sm text-slate-500" role="status">{{ activities().length ? 'No hay actividades que coincidan con la búsqueda.' : 'El catálogo no contiene actividades.' }}</div>
                    } @else {
                      <ul class="m-0 grid list-none gap-2 p-0 sm:grid-cols-2" aria-label="Actividades disponibles">
                        @for (activity of filteredActivities(); track activity.id) {
                          <li class="min-w-0 rounded-xl border border-slate-200 px-3 py-2.5 transition" [class.border-blue-300]="isSelected(activity.id)" [class.bg-blue-50]="isSelected(activity.id)">
                            <mat-checkbox [checked]="isSelected(activity.id)" [disabled]="!canManage() || commandBusy()" (change)="togglePermission(activity.id, $event.checked)" [attr.aria-label]="activity.key + ': ' + activity.name">
                              <span class="block text-sm font-semibold text-slate-800">{{ activity.name }}</span>
                              <span class="block break-all font-mono text-[11px] text-slate-500">{{ activity.key }}</span>
                            </mat-checkbox>
                            <p class="mb-0 ml-8 mt-1 text-[11px] text-slate-500">{{ activity.resource }} · {{ activity.action }}</p>
                          </li>
                        }
                      </ul>
                    }
                    <div class="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4">
                      <span class="text-xs text-slate-500">{{ permissionsChanged() ? 'Hay cambios sin guardar' : 'Permisos sincronizados' }}</span>
                      @if (canManage()) {
                        <button mat-flat-button type="button" (click)="savePermissions()" [disabled]="commandBusy() || !permissionsChanged() || permissionsLoading() || activitiesLoading()">
                          @if (commandBusy()) { <mat-spinner diameter="18" /> } @else { <mat-icon aria-hidden="true">save</mat-icon> }
                          Guardar permisos
                        </button>
                      }
                    </div>
                  }
                </div>
              </div>
            }
          </section>
        </div>
      </main>
    </div>
  `,
})
export class ProfileManagementPage implements OnInit {
  private readonly loadAllProfilesUseCase = inject(LoadAllProfilesUseCase);
  private readonly listActivitiesUseCase = inject(ListProfileActivitiesUseCase);
  private readonly getPermissionsUseCase = inject(GetProfilePermissionsUseCase);
  private readonly createProfileUseCase = inject(CreateProfileUseCase);
  private readonly updateNameUseCase = inject(UpdateProfileNameUseCase);
  private readonly changeStatusUseCase = inject(ChangeProfileStatusUseCase);
  private readonly replacePermissionsUseCase = inject(ReplaceProfilePermissionsUseCase);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);
  private readonly dialog = inject(MatDialog);
  private readonly viewContainerRef = inject(ViewContainerRef);
  private readonly destroyRef = inject(DestroyRef);
  private permissionRequest = 0;

  protected readonly canManage = computed(() => this.auth.hasPermission('PERFILES_ADMINISTRAR'));
  protected readonly profiles = signal<ProfileAdministration[]>([]);
  protected readonly selectedProfile = signal<ProfileAdministration | null>(null);
  protected readonly activities = signal<ProfileActivity[]>([]);
  protected readonly originalPermissions = signal<ProfilePermissions | null>(null);
  protected readonly selectedIds = signal<number[]>([]);
  protected readonly profilesLoading = signal(false);
  protected readonly profilesError = signal<string | null>(null);
  protected readonly activitiesLoading = signal(false);
  protected readonly activitiesError = signal<string | null>(null);
  protected readonly permissionsLoading = signal(false);
  protected readonly permissionsError = signal<string | null>(null);
  protected readonly commandBusy = signal(false);
  protected readonly profileSearch = new FormControl('', { nonNullable: true });
  protected readonly activitySearch = new FormControl('', { nonNullable: true });
  protected readonly profileSearchTerm = signal('');
  protected readonly activitySearchTerm = signal('');
  protected readonly profileName = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] });
  protected readonly nameDraft = signal('');
  protected readonly filteredProfiles = computed(() => {
    const term = this.profileSearchTerm().trim().toLocaleLowerCase();
    return term ? this.profiles().filter((profile) => profile.name.toLocaleLowerCase().includes(term)) : this.profiles();
  });
  protected readonly filteredActivities = computed(() => {
    const term = this.activitySearchTerm().trim().toLocaleLowerCase();
    return term ? this.activities().filter((activity) => `${activity.key} ${activity.name} ${activity.resource} ${activity.action}`.toLocaleLowerCase().includes(term)) : this.activities();
  });
  protected readonly nameChanged = computed(() => {
    const profile = this.selectedProfile();
    return !!profile && this.nameDraft().trim() !== profile.name;
  });
  protected readonly permissionsChanged = computed(() => {
    const current = this.originalPermissions()?.permissions.map((permission) => permission.id) ?? [];
    const selected = this.selectedIds();
    return current.length !== selected.length || current.some((id) => !selected.includes(id));
  });

  ngOnInit(): void {
    this.profileSearch.valueChanges.pipe(debounceTime(150), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef)).subscribe((value) => this.profileSearchTerm.set(value));
    this.activitySearch.valueChanges.pipe(debounceTime(150), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef)).subscribe((value) => this.activitySearchTerm.set(value));
    this.profileName.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((value) => this.nameDraft.set(value));
    this.loadProfiles();
    this.loadActivities();
  }

  protected loadProfiles(preferredId?: number): void {
    this.profilesLoading.set(true);
    this.profilesError.set(null);
    this.loadAllProfilesUseCase.execute().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (profiles) => {
        this.profiles.set(profiles);
        this.profilesLoading.set(false);
        const selectedId = preferredId ?? this.selectedProfile()?.id;
        const selected = profiles.find((profile) => profile.id === selectedId);
        if (selected) this.selectProfile(selected);
        else if (!this.selectedProfile() && profiles.length) this.selectProfile(profiles[0]);
        else if (!profiles.length) this.clearSelection();
      },
      error: (error: unknown) => {
        this.profilesLoading.set(false);
        this.profilesError.set(userFacingApiError(error, 'No fue posible consultar los perfiles.'));
      },
    });
  }

  protected loadActivities(): void {
    this.activitiesLoading.set(true);
    this.activitiesError.set(null);
    this.listActivitiesUseCase.execute().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (activities) => { this.activities.set(activities); this.activitiesLoading.set(false); },
      error: (error: unknown) => {
        this.activitiesLoading.set(false);
        this.activitiesError.set(userFacingApiError(error, 'No fue posible consultar las actividades.'));
      },
    });
  }

  protected selectProfile(profile: ProfileAdministration): void {
    this.selectedProfile.set(profile);
    this.profileName.setValue(profile.name, { emitEvent: false });
    this.nameDraft.set(profile.name);
    this.profileName.markAsPristine();
    this.selectedIds.set([]);
    this.originalPermissions.set(null);
    this.permissionsError.set(null);
    this.loadSelectedPermissions();
  }

  protected loadSelectedPermissions(): void {
    const profile = this.selectedProfile();
    if (!profile) return;
    const request = ++this.permissionRequest;
    this.permissionsLoading.set(true);
    this.permissionsError.set(null);
    this.getPermissionsUseCase.execute(profile.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (detail) => {
        if (request !== this.permissionRequest || detail.profileId !== this.selectedProfile()?.id) return;
        this.originalPermissions.set(detail);
        this.selectedIds.set(detail.permissions.map((permission) => permission.id));
        this.permissionsLoading.set(false);
      },
      error: (error: unknown) => {
        if (request !== this.permissionRequest) return;
        this.permissionsLoading.set(false);
        this.permissionsError.set(userFacingApiError(error, 'No fue posible consultar los permisos del perfil.'));
      },
    });
  }

  protected isSelected(activityId: number): boolean { return this.selectedIds().includes(activityId); }

  protected togglePermission(activityId: number, checked: boolean): void {
    const ids = new Set(this.selectedIds());
    if (checked) ids.add(activityId); else ids.delete(activityId);
    this.selectedIds.set([...ids]);
  }

  protected openCreate(): void {
    if (!this.canManage()) return;
    this.dialog.open(ProfileCreateDialog, {
      viewContainerRef: this.viewContainerRef,
      width: 'min(94vw, 480px)',
      maxWidth: 'calc(100vw - 24px)',
      autoFocus: 'first-tabbable',
      restoreFocus: true,
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((profile: ProfileAdministration | undefined) => {
      if (!profile) return;
      this.notifications.success('Perfil creado correctamente.');
      this.loadProfiles(profile.id);
    });
  }

  protected saveName(): void {
    const profile = this.selectedProfile();
    const name = this.nameDraft().trim();
    if (!profile || !this.canManage() || this.commandBusy()) return;
    if (!name || name.length > 80) { this.profileName.markAsTouched(); return; }
    if (name === profile.name) return;
    this.runCommand(this.updateNameUseCase.execute(profile.id, name), 'Perfil actualizado correctamente.', (updated) => this.replaceProfile(updated));
  }

  protected toggleStatus(): void {
    const profile = this.selectedProfile();
    if (!profile || !this.canManage() || this.commandBusy()) return;
    const status: ProfileStatus = profile.status === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    this.confirm.ask({
      title: status === 'INACTIVO' ? 'Inactivar perfil' : 'Activar perfil',
      message: status === 'INACTIVO'
        ? 'Los usuarios con este perfil no podrán iniciar sesión. Si es el último administrador efectivo, el backend rechazará la operación.'
        : 'Los usuarios asignados a este perfil podrán iniciar sesión según el estado de sus cuentas.',
      confirmLabel: status === 'INACTIVO' ? 'Inactivar perfil' : 'Activar perfil',
      kind: status === 'INACTIVO' ? 'warning' : undefined,
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe((accepted) => {
      if (accepted) this.runCommand(this.changeStatusUseCase.execute(profile.id, status), 'Estado del perfil actualizado.', (updated) => this.replaceProfile(updated));
    });
  }

  protected savePermissions(): void {
    const profile = this.selectedProfile();
    if (!profile || !this.canManage() || this.commandBusy() || !this.permissionsChanged()) return;
    const finalIds = [...this.selectedIds()];
    this.runCommand(this.replacePermissionsUseCase.execute(profile.id, finalIds), 'Permisos actualizados correctamente.', (detail) => {
      this.originalPermissions.set(detail);
      this.selectedIds.set(detail.permissions.map((permission) => permission.id));
      this.replaceProfile({ ...profile, permissionCount: detail.permissions.length });
    });
  }

  private runCommand<T>(request: import('rxjs').Observable<T>, successMessage: string, onSuccess: (result: T) => void): void {
    this.commandBusy.set(true);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (result) => {
        this.commandBusy.set(false);
        onSuccess(result);
        this.notifications.success(successMessage);
      },
      error: (error: unknown) => {
        this.commandBusy.set(false);
        this.notifications.error(userFacingApiError(error, 'No fue posible completar la operación.'));
      },
    });
  }

  private replaceProfile(profile: ProfileAdministration): void {
    this.profiles.update((profiles) => profiles.map((item) => item.id === profile.id ? profile : item));
    this.selectedProfile.set(profile);
    this.profileName.setValue(profile.name, { emitEvent: false });
    this.nameDraft.set(profile.name);
    this.profileName.markAsPristine();
  }

  private clearSelection(): void {
    ++this.permissionRequest;
    this.selectedProfile.set(null);
    this.originalPermissions.set(null);
    this.selectedIds.set([]);
    this.permissionsLoading.set(false);
    this.permissionsError.set(null);
    this.profileName.reset('');
  }
}
