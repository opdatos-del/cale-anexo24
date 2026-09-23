import { ApplicationRef, ComponentRef, createComponent, createEnvironmentInjector, EnvironmentInjector } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, Subject, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ConfirmService } from '../../../../../../core/ui/confirm-dialog/confirm.service';
import { NotificationService } from '../../../../../../core/notifications/notification.service';
import { ChangeUserExpirationUseCase } from '../../../application/use-cases/change-user-expiration.use-case';
import { ChangeUserProfileUseCase } from '../../../application/use-cases/change-user-profile.use-case';
import { ChangeUserStatusUseCase } from '../../../application/use-cases/change-user-status.use-case';
import { CreateUserUseCase } from '../../../application/use-cases/create-user.use-case';
import { GetUserUseCase } from '../../../application/use-cases/get-user.use-case';
import { ResetUserPasswordUseCase } from '../../../application/use-cases/reset-user-password.use-case';
import { SearchUsersUseCase } from '../../../application/use-cases/search-users.use-case';
import { UpdateUserUseCase } from '../../../application/use-cases/update-user.use-case';
import { UserAdministration, UserPage, UserStatus } from '../../../domain/models/user-administration.model';
import { LoadAllProfilesUseCase } from '../../../../profiles/application/use-cases/load-all-profiles.use-case';
import { UserRepository } from '../../../domain/repositories/user.repository';
import { UserCreateDialog } from '../../dialogs/user-create/user-create.dialog';
import { UserEditDialog } from '../../dialogs/user-edit/user-edit.dialog';
import { UserExpirationDialog } from '../../dialogs/user-expiration/user-expiration.dialog';
import { UserPasswordDialog } from '../../dialogs/user-password/user-password.dialog';
import { UserProfileDialog } from '../../dialogs/user-profile/user-profile.dialog';
import { UserListPage } from './user-list.page';

interface PageInternals {
  filters: {
    controls: { name: { setValue(value: string): void }; status: { setValue(value: UserStatus): void }; profileId: { setValue(value: number | null): void } };
    setValue(value: { key: string; name: string; email: string; status: UserStatus; profileId: number | null }): void;
    getRawValue(): { key: string; name: string; email: string; status: UserStatus | null; profileId: number | null };
  };
  loading(): boolean;
  users(): UserAdministration[];
  error(): string | null;
  load(): void;
  clearFilters(): void;
  onPage(event: { pageIndex: number; pageSize: number }): void;
  confirmStatusChange(user: UserAdministration): void;
  openCreate(): void;
  openEdit(user: UserAdministration): void;
  openProfile(user: UserAdministration): void;
  openExpiration(user: UserAdministration): void;
  openPassword(user: UserAdministration): void;
}

const user: UserAdministration = { id: 7, key: 'OPERADOR', name: 'Operador', email: 'operador@example.test', status: 'ACTIVO', expiration: null, profileId: 3, profileName: 'Operación' };
const firstPage: UserPage = { items: [user], total: 1, page: 1, pageSize: 20 };

describe('UserListPage', () => {
  let fixture: ComponentFixture<UserListPage>;
  let component: UserListPage;
  const search = { execute: vi.fn() };
  const getUser = { execute: vi.fn() };
  const changeStatus = { execute: vi.fn() };
  const notifications = { success: vi.fn(), error: vi.fn() };
  const confirm = { ask: vi.fn() };
  const dialog = { open: vi.fn() };
  const loadProfiles = { execute: vi.fn() };

  function createPage(): void {
    TestBed.configureTestingModule({
      imports: [UserListPage],
      providers: [
        provideNoopAnimations(),
        { provide: SearchUsersUseCase, useValue: search },
        { provide: GetUserUseCase, useValue: getUser },
        { provide: ChangeUserStatusUseCase, useValue: changeStatus },
        { provide: NotificationService, useValue: notifications },
        { provide: ConfirmService, useValue: confirm },
        { provide: MatDialog, useValue: dialog },
        { provide: LoadAllProfilesUseCase, useValue: loadProfiles },
      ],
    });
    fixture = TestBed.createComponent(UserListPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.useFakeTimers();
    search.execute.mockReset();
    getUser.execute.mockReset();
    changeStatus.execute.mockReset();
    notifications.success.mockReset();
    notifications.error.mockReset();
    confirm.ask.mockReset();
    dialog.open.mockReset();
    loadProfiles.execute.mockReset();
    loadProfiles.execute.mockReturnValue(of([]));
  });

  afterEach(() => vi.useRealTimers());

  it('carga inicialmente y muestra el estado de carga hasta recibir la página', () => {
    const response = new Subject<UserPage>();
    search.execute.mockReturnValue(response);
    createPage();
    const instance = component as unknown as PageInternals;

    expect(search.execute).toHaveBeenCalledWith({ key: '', name: '', email: '', status: null, profileId: null, page: 1, pageSize: 20 });
    expect(instance.loading()).toBe(true);
    response.next(firstPage);
    expect(instance.loading()).toBe(false);
    expect(instance.users()).toEqual([user]);
  });

  it('aplica filtros de texto con debounce, estado inmediato y reinicia la página', () => {
    search.execute.mockReturnValue(of(firstPage));
    createPage();
    const instance = component as unknown as PageInternals;
    instance.onPage({ pageIndex: 2, pageSize: 50 });
    instance.filters.controls.name.setValue('Operador');
    vi.advanceTimersByTime(499);
    expect(search.execute).toHaveBeenCalledTimes(2);
    vi.advanceTimersByTime(1);
    expect(search.execute).toHaveBeenLastCalledWith({ key: '', name: 'Operador', email: '', status: null, profileId: null, page: 1, pageSize: 20 });

    instance.filters.controls.status.setValue('INACTIVO');
    expect(search.execute).toHaveBeenLastCalledWith({ key: '', name: 'Operador', email: '', status: 'INACTIVO', profileId: null, page: 1, pageSize: 20 });

    instance.filters.controls.profileId.setValue(3);
    expect(search.execute).toHaveBeenLastCalledWith({ key: '', name: 'Operador', email: '', status: 'INACTIVO', profileId: 3, page: 1, pageSize: 20 });
  });

  it('cambia página, limpia filtros y vuelve a la primera página', () => {
    search.execute.mockReturnValue(of(firstPage));
    createPage();
    const instance = component as unknown as PageInternals;
    instance.filters.setValue({ key: 'CLAVE', name: 'Nombre', email: 'correo@example.test', status: 'ACTIVO', profileId: null });
    instance.onPage({ pageIndex: 3, pageSize: 100 });
    expect(search.execute).toHaveBeenLastCalledWith({ key: 'CLAVE', name: 'Nombre', email: 'correo@example.test', status: 'ACTIVO', profileId: null, page: 4, pageSize: 100 });

    instance.clearFilters();
    expect(instance.filters.getRawValue()).toEqual({ key: '', name: '', email: '', status: null, profileId: null });
    expect(search.execute).toHaveBeenLastCalledWith({ key: '', name: '', email: '', status: null, profileId: null, page: 1, pageSize: 20 });
  });

  it('presenta un error seguro y descarta respuestas obsoletas', () => {
    const firstResponse = new Subject<UserPage>();
    const secondResponse = new Subject<UserPage>();
    search.execute.mockReturnValueOnce(firstResponse).mockReturnValueOnce(secondResponse).mockReturnValue(throwError(() => new Error('fallo')));
    createPage();
    const instance = component as unknown as PageInternals;
    instance.filters.controls.status.setValue('INACTIVO');
    firstResponse.next({ ...firstPage, items: [{ ...user, name: 'Obsoleto' }] });
    secondResponse.next({ ...firstPage, items: [{ ...user, name: 'Vigente' }] });
    expect(instance.users()).toEqual([{ ...user, name: 'Vigente' }]);

    instance.load();
    expect(instance.loading()).toBe(false);
    expect(instance.error()).toBe('No fue posible consultar los usuarios.');
    expect(notifications.error).toHaveBeenCalledWith('No fue posible consultar los usuarios.');
  });

  it('solicita confirmación y cambia el estado sólo al aceptarla', () => {
    search.execute.mockReturnValue(of(firstPage));
    confirm.ask.mockReturnValue(of(true));
    changeStatus.execute.mockReturnValue(of({ ...user, status: 'INACTIVO' }));
    createPage();
    (component as unknown as PageInternals).confirmStatusChange(user);

    expect(confirm.ask).toHaveBeenCalledWith(expect.objectContaining({ confirmLabel: 'Inactivar', kind: 'danger' }));
    expect(changeStatus.execute).toHaveBeenCalledWith(7, { status: 'INACTIVO' });
    expect(notifications.success).toHaveBeenCalledWith('Usuario inactivado.');
  });

  it('resuelve los casos de uso de diálogos desde el inyector hijo asociado a la ruta', () => {
    const repository = {
      search: vi.fn(() => of(firstPage)), getById: vi.fn(() => of(user)), create: vi.fn(() => of(user)), update: vi.fn(() => of(user)),
      changeStatus: vi.fn(() => of(user)), changeProfile: vi.fn(() => of(user)), changeExpiration: vi.fn(() => of(user)), resetPassword: vi.fn(() => of(void 0)),
    };
    TestBed.configureTestingModule({
      imports: [UserListPage, MatDialogModule],
      providers: [provideNoopAnimations(), { provide: NotificationService, useValue: notifications }, { provide: ConfirmService, useValue: confirm }],
    });
    const routeInjector = createEnvironmentInjector([
      { provide: UserRepository, useValue: repository }, SearchUsersUseCase, GetUserUseCase, CreateUserUseCase, UpdateUserUseCase,
      ChangeUserStatusUseCase, ChangeUserProfileUseCase, ChangeUserExpirationUseCase, ResetUserPasswordUseCase,
      { provide: LoadAllProfilesUseCase, useValue: loadProfiles },
    ], TestBed.inject(EnvironmentInjector));
    const appRef = TestBed.inject(ApplicationRef);
    const componentRef: ComponentRef<UserListPage> = createComponent(UserListPage, { environmentInjector: routeInjector });
    appRef.attachView(componentRef.hostView);
    componentRef.changeDetectorRef.detectChanges();
    component = componentRef.instance;
    const actualDialog = TestBed.inject(MatDialog);
    const instance = component as unknown as PageInternals;

    instance.openCreate();
    expect(actualDialog.openDialogs.at(-1)?.componentInstance).toBeInstanceOf(UserCreateDialog);
    instance.openEdit(user);
    expect(actualDialog.openDialogs.at(-1)?.componentInstance).toBeInstanceOf(UserEditDialog);
    instance.openProfile(user);
    expect(actualDialog.openDialogs.at(-1)?.componentInstance).toBeInstanceOf(UserProfileDialog);
    instance.openExpiration(user);
    expect(actualDialog.openDialogs.at(-1)?.componentInstance).toBeInstanceOf(UserExpirationDialog);
    instance.openPassword(user);
    expect(actualDialog.openDialogs.at(-1)?.componentInstance).toBeInstanceOf(UserPasswordDialog);

    actualDialog.closeAll();
    appRef.detachView(componentRef.hostView);
    componentRef.destroy();
    routeInjector.destroy();
  });
});
