import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NotificationService } from '../../../../../../core/notifications/notification.service';
import { LoadAllProfilesUseCase } from '../../../../profiles/application/use-cases/load-all-profiles.use-case';
import { ChangeUserProfileUseCase } from '../../../application/use-cases/change-user-profile.use-case';
import { UserAdministration } from '../../../domain/models/user-administration.model';
import { UserProfileDialog } from './user-profile.dialog';

interface ProfileDialogInternals {
  form: { controls: { profileId: { value: number | null; setValue(value: number | null): void } } };
  save(): void;
}

const user: UserAdministration = {
  id: 7,
  key: 'ANA',
  name: 'Ana',
  email: 'ana@example.test',
  status: 'ACTIVO',
  expiration: null,
  profileId: 1,
  profileName: 'Perfil anterior',
};

describe('UserProfileDialog', () => {
  let fixture: ComponentFixture<UserProfileDialog>;
  let component: UserProfileDialog;
  const changeProfile = { execute: vi.fn() };
  const loadProfiles = { execute: vi.fn() };
  const dialogRef = { close: vi.fn() };
  const notifications = { success: vi.fn() };

  beforeEach(() => {
    changeProfile.execute.mockReset();
    loadProfiles.execute.mockReset();
    dialogRef.close.mockReset();
    notifications.success.mockReset();
    TestBed.configureTestingModule({
      imports: [UserProfileDialog],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: user },
        { provide: ChangeUserProfileUseCase, useValue: changeProfile },
        { provide: LoadAllProfilesUseCase, useValue: loadProfiles },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: NotificationService, useValue: notifications },
      ],
    });
  });

  it('preselecciona el perfil actual cuando sigue activo y no guarda sin cambios', () => {
    loadProfiles.execute.mockReturnValue(of([{ id: 1, name: 'Perfil anterior', status: 'ACTIVO', permissionCount: 1 }]));
    fixture = TestBed.createComponent(UserProfileDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const instance = component as unknown as ProfileDialogInternals;
    expect(instance.form.controls.profileId.value).toBe(1);
    instance.save();
    expect(changeProfile.execute).not.toHaveBeenCalled();
  });

  it('exige una nueva selección cuando el perfil actual está inactivo', () => {
    loadProfiles.execute.mockReturnValue(of([{ id: 2, name: 'Operación', status: 'ACTIVO', permissionCount: 1 }]));
    fixture = TestBed.createComponent(UserProfileDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const instance = component as unknown as ProfileDialogInternals;
    expect(instance.form.controls.profileId.value).toBeNull();
    instance.form.controls.profileId.setValue(2);
    const updated = { ...user, profileId: 2, profileName: 'Operación' };
    changeProfile.execute.mockReturnValue(of(updated));
    instance.save();

    expect(changeProfile.execute).toHaveBeenCalledWith(7, { profileId: 2 });
    expect(notifications.success).toHaveBeenCalledWith('Perfil actualizado correctamente.');
    expect(dialogRef.close).toHaveBeenCalledWith(updated);
  });
});
