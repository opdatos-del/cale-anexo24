import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NotificationService } from '../../../../../../core/notifications/notification.service';
import { ResetUserPasswordUseCase } from '../../../application/use-cases/reset-user-password.use-case';
import { UserAdministration } from '../../../domain/models/user-administration.model';
import { STRONG_PASSWORD_PATTERN, UserPasswordDialog } from './user-password.dialog';

interface PasswordDialogInternals {
  form: { setValue(value: { password: string; confirmation: string }): void; getRawValue(): { password: string; confirmation: string }; hasError(error: string): boolean };
  save(): void;
  close(): void;
}

const user: UserAdministration = { id: 7, key: 'OPERADOR', name: 'Operador', email: 'operador@example.test', status: 'ACTIVO', expiration: null, profileId: 3, profileName: 'Operación' };

describe('UserPasswordDialog', () => {
  let fixture: ComponentFixture<UserPasswordDialog>;
  let component: UserPasswordDialog;
  const resetPassword = { execute: vi.fn() };
  const dialogRef = { close: vi.fn() };
  const notifications = { success: vi.fn() };

  beforeEach(() => {
    resetPassword.execute.mockReset();
    dialogRef.close.mockReset();
    notifications.success.mockReset();
    TestBed.configureTestingModule({
      imports: [UserPasswordDialog],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: user },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: ResetUserPasswordUseCase, useValue: resetPassword },
        { provide: NotificationService, useValue: notifications },
      ],
    });
    fixture = TestBed.createComponent(UserPasswordDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('aplica la política y detecta contraseñas distintas', () => {
    expect(STRONG_PASSWORD_PATTERN.test('sin-mayuscula1!')).toBe(false);
    expect(STRONG_PASSWORD_PATTERN.test('TemporalSegura1!')).toBe(true);
    const instance = component as unknown as PasswordDialogInternals;
    instance.form.setValue({ password: 'TemporalSegura1!', confirmation: 'OtraSegura2!' });
    expect(instance.form.hasError('passwordsMismatch')).toBe(true);
    instance.save();
    expect(resetPassword.execute).not.toHaveBeenCalled();
  });

  it('restablece sin exponer la contraseña, cierra y limpia controles', () => {
    resetPassword.execute.mockReturnValue(of(void 0));
    const instance = component as unknown as PasswordDialogInternals;
    instance.form.setValue({ password: 'TemporalSegura1!', confirmation: 'TemporalSegura1!' });
    instance.save();

    expect(resetPassword.execute).toHaveBeenCalledWith(7, { password: expect.any(String) });
    expect(notifications.success).toHaveBeenCalledWith('Contraseña restablecida correctamente.');
    expect(dialogRef.close).toHaveBeenCalledWith(true);
    expect(instance.form.getRawValue()).toEqual({ password: '', confirmation: '' });
  });

  it('limpia ambos campos al cancelar', () => {
    const instance = component as unknown as PasswordDialogInternals;
    instance.form.setValue({ password: 'TemporalSegura1!', confirmation: 'TemporalSegura1!' });
    instance.close();
    expect(instance.form.getRawValue()).toEqual({ password: '', confirmation: '' });
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
