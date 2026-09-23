import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NotificationService } from '../../../../../../core/notifications/notification.service';
import { LoadAllProfilesUseCase } from '../../../../profiles/application/use-cases/load-all-profiles.use-case';
import { CreateUserUseCase } from '../../../application/use-cases/create-user.use-case';
import { UserCreateDialog } from './user-create.dialog';

interface CreateDialogInternals {
  form: { setValue(value: { key: string; name: string; email: string; password: string; confirmation: string; profileId: number | null; expiration: Date | null }): void; getRawValue(): unknown };
  save(): void;
}

describe('UserCreateDialog', () => {
  let fixture: ComponentFixture<UserCreateDialog>;
  let component: UserCreateDialog;
  const create = { execute: vi.fn() };
  const loadProfiles = { execute: vi.fn() };
  const dialogRef = { close: vi.fn() };
  const notifications = { success: vi.fn() };

  beforeEach(() => {
    create.execute.mockReset();
    loadProfiles.execute.mockReset();
    dialogRef.close.mockReset();
    notifications.success.mockReset();
    loadProfiles.execute.mockReturnValue(of([{ id: 2, name: 'Operación', status: 'ACTIVO', permissionCount: 1 }]));
    TestBed.configureTestingModule({
      imports: [UserCreateDialog],
      providers: [
        { provide: CreateUserUseCase, useValue: create },
        { provide: LoadAllProfilesUseCase, useValue: loadProfiles },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: NotificationService, useValue: notifications },
      ],
    });
    fixture = TestBed.createComponent(UserCreateDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('carga únicamente perfiles activos', () => {
    expect(loadProfiles.execute).toHaveBeenCalledWith('ACTIVO');
  });

  it('recorta los campos y nunca incluye confirmación al crear', () => {
    const created = { id: 7, key: 'clave', name: 'Ana', email: 'ana@example.test', status: 'ACTIVO', expiration: null, profileId: 2, profileName: 'Operación' };
    create.execute.mockReturnValue(of(created));
    const instance = component as unknown as CreateDialogInternals;
    instance.form.setValue({ key: ' clave ', name: ' Ana ', email: 'ana@example.test', password: 'ClaveSegura1!', confirmation: 'ClaveSegura1!', profileId: 2, expiration: new Date(2032, 1, 29) });

    expect((instance as unknown as { form: { valid: boolean } }).form.valid).toBe(true);
    instance.save();

    expect(create.execute).toHaveBeenCalledWith({ key: 'clave', name: 'Ana', email: 'ana@example.test', password: 'ClaveSegura1!', profileId: 2, expiration: '2032-02-29' });
    expect(notifications.success).toHaveBeenCalledWith('Usuario creado correctamente.');
    expect(dialogRef.close).toHaveBeenCalledWith(created);
  });

});
