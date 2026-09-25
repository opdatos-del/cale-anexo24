import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NotificationService } from '@core/notifications/notification.service';
import { ChangeUserExpirationUseCase } from '@features/administration/users/application/use-cases/change-user-expiration.use-case';
import { UserAdministration } from '@features/administration/users/domain/models/user-administration.model';
import { UserExpirationDialog, toLocalDate } from './user-expiration.dialog';

interface ExpirationDialogInternals {
  form: { controls: { expiration: { value: Date | null; setValue(value: Date | null): void } } };
  save(): void;
  close(): void;
}

const user: UserAdministration = { id: 7, key: 'OPERADOR', name: 'Operador', email: 'operador@example.test', status: 'ACTIVO', expiration: '2030-01-02', profileId: 3, profileName: 'Operación' };

describe('UserExpirationDialog', () => {
  let fixture: ComponentFixture<UserExpirationDialog>;
  let component: UserExpirationDialog;
  const changeExpiration = { execute: vi.fn() };
  const dialogRef = { close: vi.fn() };
  const notifications = { success: vi.fn() };

  beforeEach(() => {
    changeExpiration.execute.mockReset();
    dialogRef.close.mockReset();
    notifications.success.mockReset();
    TestBed.configureTestingModule({
      imports: [UserExpirationDialog],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: user },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: ChangeUserExpirationUseCase, useValue: changeExpiration },
        { provide: NotificationService, useValue: notifications },
      ],
    });
    fixture = TestBed.createComponent(UserExpirationDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('convierte fechas locales sin desplazar la fecha civil', () => {
    expect(toLocalDate(new Date(2031, 10, 9))).toBe('2031-11-09');
    expect(toLocalDate(null)).toBeNull();
  });

  it('envía una fecha civil y limpia el formulario al guardar', () => {
    const updated = { ...user, expiration: '2031-11-09' };
    changeExpiration.execute.mockReturnValue(of(updated));
    const instance = component as unknown as ExpirationDialogInternals;
    instance.form.controls.expiration.setValue(new Date(2031, 10, 9));
    instance.save();

    expect(changeExpiration.execute).toHaveBeenCalledWith(7, { expiration: '2031-11-09' });
    expect(notifications.success).toHaveBeenCalledWith('Vigencia actualizada.');
    expect(dialogRef.close).toHaveBeenCalledWith(updated);
    expect(instance.form.controls.expiration.value).toBeNull();
  });

  it('permite guardar una vigencia indefinida y limpia al cancelar', () => {
    changeExpiration.execute.mockReturnValue(of({ ...user, expiration: null }));
    const instance = component as unknown as ExpirationDialogInternals;
    instance.form.controls.expiration.setValue(null);
    instance.save();
    expect(changeExpiration.execute).toHaveBeenCalledWith(7, { expiration: null });

    instance.form.controls.expiration.setValue(new Date(2031, 0, 1));
    instance.close();
    expect(instance.form.controls.expiration.value).toBeNull();
  });
});
