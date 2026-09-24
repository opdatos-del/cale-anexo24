import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notifications/notification.service';
import { ConfirmService } from '@core/ui/confirm-dialog/confirm.service';
import { of, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ChangeProfileStatusUseCase } from '../../../application/use-cases/change-profile-status.use-case';
import { CreateProfileUseCase } from '../../../application/use-cases/create-profile.use-case';
import { GetProfilePermissionsUseCase } from '../../../application/use-cases/get-profile-permissions.use-case';
import { ListProfileActivitiesUseCase } from '../../../application/use-cases/list-profile-activities.use-case';
import { LoadAllProfilesUseCase } from '../../../application/use-cases/load-all-profiles.use-case';
import { ReplaceProfilePermissionsUseCase } from '../../../application/use-cases/replace-profile-permissions.use-case';
import { UpdateProfileNameUseCase } from '../../../application/use-cases/update-profile-name.use-case';
import { ProfileAdministration } from '../../../domain/models/profile-administration.model';
import { ProfileManagementPage } from './profile-management.page';

const TEST_PROFILE = { id: 17, name: 'Operaciones', status: 'ACTIVO' as const, permissionCount: 1 };
const activities = [
  { id: 3, key: 'OPERACIONES_CONSULTAR', name: 'Consultar operaciones', resource: 'operaciones', action: 'CONSULTAR' },
  { id: 9, key: 'PERFILES_ADMINISTRAR', name: 'Administrar perfiles', resource: 'perfiles', action: 'ADMINISTRAR' },
];

interface PageInternals {
  selectProfile(profile: ProfileAdministration): void;
  loadProfiles(): void;
  saveName(): void;
  savePermissions(): void;
  togglePermission(id: number, checked: boolean): void;
  toggleStatus(): void;
  selectedIds(): number[];

  permissionsChanged(): boolean;
  commandBusy(): boolean;
  selectedProfile(): ProfileAdministration | null;
}

describe('ProfileManagementPage', () => {
  let fixture: ComponentFixture<ProfileManagementPage>;
  let page: PageInternals;
  const loadProfiles = { execute: vi.fn() };
  const listActivities = { execute: vi.fn() };
  const getPermissions = { execute: vi.fn() };
  const createProfile = { execute: vi.fn() };
  const updateName = { execute: vi.fn() };
  const changeStatus = { execute: vi.fn() };
  const replacePermissions = { execute: vi.fn() };
  const notifications = { success: vi.fn(), error: vi.fn() };
  const dialog = { open: vi.fn() };
  const confirm = { ask: vi.fn() };
  const auth = { hasPermission: vi.fn(() => true) };

  beforeEach(() => {
    for (const useCase of [loadProfiles, listActivities, getPermissions, createProfile, updateName, changeStatus, replacePermissions]) useCase.execute.mockReset();
    notifications.success.mockReset(); notifications.error.mockReset(); dialog.open.mockReset(); confirm.ask.mockReset(); auth.hasPermission.mockReturnValue(true);
    loadProfiles.execute.mockReturnValue(of([TEST_PROFILE]));
    listActivities.execute.mockReturnValue(of(activities));
    getPermissions.execute.mockReturnValue(of({ profileId: 17, permissions: [activities[0]] }));
    updateName.execute.mockReturnValue(of({ ...TEST_PROFILE, name: 'Operación' }));
    changeStatus.execute.mockReturnValue(of({ ...TEST_PROFILE, status: 'INACTIVO' }));
    replacePermissions.execute.mockImplementation((_id: number, ids: number[]) => of({ profileId: 17, permissions: activities.filter((activity) => ids.includes(activity.id)) }));
    TestBed.configureTestingModule({
      imports: [ProfileManagementPage],
      providers: [
        { provide: LoadAllProfilesUseCase, useValue: loadProfiles },
        { provide: ListProfileActivitiesUseCase, useValue: listActivities },
        { provide: GetProfilePermissionsUseCase, useValue: getPermissions },
        { provide: CreateProfileUseCase, useValue: createProfile },
        { provide: UpdateProfileNameUseCase, useValue: updateName },
        { provide: ChangeProfileStatusUseCase, useValue: changeStatus },
        { provide: ReplaceProfilePermissionsUseCase, useValue: replacePermissions },
        { provide: NotificationService, useValue: notifications },
        { provide: MatDialog, useValue: dialog },
        { provide: ConfirmService, useValue: confirm },
        { provide: AuthService, useValue: auth },
      ],
    });
    fixture = TestBed.createComponent(ProfileManagementPage);
    page = fixture.componentInstance as unknown as PageInternals;
    fixture.detectChanges();
  });

  it('carga lista, actividades, perfil seleccionado y permisos desde backend', () => {
    expect(loadProfiles.execute).toHaveBeenCalledOnce();
    expect(listActivities.execute).toHaveBeenCalledOnce();
    expect(getPermissions.execute).toHaveBeenCalledWith(17);
    expect(page.selectedProfile()?.name).toBe('Operaciones');
    expect(page.selectedIds()).toEqual([3]);
  });

  it('representa lista vacía y conserva selección vacía', () => {
    loadProfiles.execute.mockReturnValue(of([]));
    page.loadProfiles();
    fixture.detectChanges();
    expect(page.selectedProfile()).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Todavía no hay perfiles');
  });

  it('expone loading y error recuperable del listado', () => {
    const pending = new Subject<ProfileAdministration[]>();
    loadProfiles.execute.mockReturnValue(pending);
    page.loadProfiles();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[aria-busy="true"]')).not.toBeNull();
    pending.error(new HttpErrorResponse({ status: 403, error: { message: 'No autorizado.' } }));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No pudimos cargar los perfiles');
    expect(fixture.nativeElement.textContent).toContain('No tienes permiso para realizar esta operación.');
  });

  it('cambia perfil seleccionado y consulta permisos del nuevo perfil', () => {
    const second = { id: 18, name: 'Auditoría', status: 'INACTIVO' as const, permissionCount: 0 };
    getPermissions.execute.mockReturnValue(of({ profileId: 18, permissions: [] }));
    page.selectProfile(second);
    expect(page.selectedProfile()?.id).toBe(18);
    expect(getPermissions.execute).toHaveBeenLastCalledWith(18);
    expect(page.selectedIds()).toEqual([]);
  });

  it('reemplaza una sola vez con set final y omite el no-op aunque orden cambie', () => {
    page.togglePermission(9, true);
    expect(page.permissionsChanged()).toBe(true);
    page.savePermissions();
    expect(replacePermissions.execute).toHaveBeenCalledWith(17, [3, 9]);
    expect(notifications.success).toHaveBeenCalledWith('Permisos actualizados correctamente.');
    page.savePermissions();
    expect(replacePermissions.execute).toHaveBeenCalledOnce();
  });

  it('trata permisos en orden distinto como mismo conjunto y evita el no-op', () => {
    getPermissions.execute.mockReturnValue(of({ profileId: 17, permissions: activities }));
    page.selectProfile(TEST_PROFILE);
    const internals = fixture.componentInstance as unknown as { selectedIds: { set(ids: number[]): void } };
    internals.selectedIds.set([9, 3]);
    expect(page.permissionsChanged()).toBe(false);
    page.savePermissions();
    expect(replacePermissions.execute).not.toHaveBeenCalled();
  });

  it('permite dejar permisos vacíos enviando array vacío', () => {
    page.togglePermission(3, false);
    page.savePermissions();
    expect(replacePermissions.execute).toHaveBeenCalledWith(17, []);
  });

  it('renombra con valor normalizado y evita request no-op', () => {
    const input = fixture.nativeElement.querySelector('input[formcontrolname="profileName"]') as HTMLInputElement | null;
    expect(input).toBeNull();
    const formControl = (fixture.componentInstance as unknown as { profileName: { setValue(value: string): void } }).profileName;
    formControl.setValue(' Operación ');
    page.saveName();
    expect(updateName.execute).toHaveBeenCalledWith(17, 'Operación');
    formControl.setValue('Operación');
    page.saveName();
    expect(updateName.execute).toHaveBeenCalledOnce();
  });

  it('mantiene estado ante fallo backend y muestra error seguro', () => {
    updateName.execute.mockReturnValue(throwError(() => new Error('SQL secreto')));
    const formControl = (fixture.componentInstance as unknown as { profileName: { setValue(value: string): void } }).profileName;
    formControl.setValue('Otro nombre');
    page.saveName();
    expect(page.selectedProfile()?.status).toBe('ACTIVO');
    expect(notifications.error).toHaveBeenCalledWith('No fue posible completar la operación.');
  });

  it('deshabilita segunda operación mientras command sigue pendiente', () => {
    const pending = new Subject<typeof TEST_PROFILE>();
    updateName.execute.mockReturnValue(pending);
    const formControl = (fixture.componentInstance as unknown as { profileName: { setValue(value: string): void } }).profileName;
    formControl.setValue('Otro');
    page.saveName();
    page.saveName();
    expect(updateName.execute).toHaveBeenCalledOnce();
    expect(page.commandBusy()).toBe(true);
    pending.next({ ...TEST_PROFILE, name: 'Otro' }); pending.complete();
    expect(page.commandBusy()).toBe(false);
  });

  it('no cambia estado hasta aceptar confirmación y aplica respuesta confirmada', () => {
    confirm.ask.mockReturnValue(of(true));
    page.toggleStatus();
    expect(changeStatus.execute).toHaveBeenCalledWith(17, 'INACTIVO');
    expect(page.selectedProfile()?.status).toBe('INACTIVO');
  });
});
