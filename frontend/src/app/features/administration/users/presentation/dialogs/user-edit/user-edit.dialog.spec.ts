import { HttpErrorResponse } from '@angular/common/http';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NotificationService } from '@core/notifications/notification.service';
import { UpdateUserUseCase } from '@features/administration/users/application/use-cases/update-user.use-case';
import { UserAdministration } from '@features/administration/users/domain/models/user-administration.model';
import { UserEditDialog } from './user-edit.dialog';

interface EditDialogInternals {
  form: {
    controls: { email: { setValue(value: string): void; touched: boolean } };
    setValue(value: { name: string; email: string }): void;
    getRawValue(): { name: string; email: string };
  };
  save(): void;
  close(): void;
  error(): string | null;
  saving(): boolean;
}

const user: UserAdministration = { id: 7, key: 'OPERADOR', name: 'Nombre previo', email: 'previo@example.test', status: 'ACTIVO', expiration: null, profileId: 3, profileName: 'Operación' };

describe('UserEditDialog', () => {
  let fixture: ComponentFixture<UserEditDialog>;
  let component: UserEditDialog;
  const update = { execute: vi.fn() };
  const dialogRef = { close: vi.fn() };
  const notifications = { success: vi.fn(), error: vi.fn() };

  beforeEach(() => {
    update.execute.mockReset();
    dialogRef.close.mockReset();
    notifications.success.mockReset();
    TestBed.configureTestingModule({
      imports: [UserEditDialog],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: user },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: UpdateUserUseCase, useValue: update },
        { provide: NotificationService, useValue: notifications },
      ],
    });
    fixture = TestBed.createComponent(UserEditDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('marca inválido el formulario sin invocar el caso de uso', () => {
    const instance = component as unknown as EditDialogInternals;
    instance.form.controls.email.setValue('correo-invalido');
    instance.save();

    expect(update.execute).not.toHaveBeenCalled();
    expect(instance.form.controls.email.touched).toBe(true);
  });

  it('guarda datos válidos, cierra y limpia el formulario tras guardar', () => {
    const updated = { ...user, name: 'Nombre nuevo', email: 'nuevo@example.test' };
    update.execute.mockReturnValue(of(updated));
    const instance = component as unknown as EditDialogInternals;
    instance.form.setValue({ name: 'Nombre nuevo', email: 'nuevo@example.test' });
    instance.save();

    expect(update.execute).toHaveBeenCalledWith(7, { name: 'Nombre nuevo', email: 'nuevo@example.test' });
    expect(notifications.success).toHaveBeenCalledWith('Usuario actualizado.');
    expect(dialogRef.close).toHaveBeenCalledWith(updated);
    expect(instance.form.getRawValue()).toEqual({ name: '', email: '' });
  });

  it('muestra el mensaje seguro ante un conflicto sin cerrar', () => {
    update.execute.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'El correo ya está registrado.' } })));
    const instance = component as unknown as EditDialogInternals;
    instance.save();

    expect(instance.error()).toBe('El correo ya está registrado.');
    expect(instance.saving()).toBe(false);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('limpia los controles al cancelar y destruirse', () => {
    const instance = component as unknown as EditDialogInternals;
    instance.form.setValue({ name: 'Temporal', email: 'temporal@example.test' });
    instance.close();
    expect(instance.form.getRawValue()).toEqual({ name: '', email: '' });
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
