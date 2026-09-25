import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CreateProfileUseCase } from '@features/administration/profiles/application/use-cases/create-profile.use-case';
import { ProfileAdministration } from '@features/administration/profiles/domain/models/profile-administration.model';
import { ProfileCreateDialog } from './profile-create.dialog';

interface DialogInternals { name: { setValue(value: string): void; markAsTouched(): void }; save(): void; saving(): boolean; error(): string | null }

const created: ProfileAdministration = { id: 10, name: 'Operaciones', status: 'ACTIVO', permissionCount: 0 };

describe('ProfileCreateDialog', () => {
  let fixture: ComponentFixture<ProfileCreateDialog>;
  let dialog: DialogInternals;
  const create = { execute: vi.fn() };
  const dialogRef = { close: vi.fn() };

  beforeEach(() => {
    create.execute.mockReset(); dialogRef.close.mockReset();
    create.execute.mockReturnValue(of(created));
    TestBed.configureTestingModule({
      imports: [ProfileCreateDialog],
      providers: [
        { provide: CreateProfileUseCase, useValue: create },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    });
    fixture = TestBed.createComponent(ProfileCreateDialog);
    dialog = fixture.componentInstance as unknown as DialogInternals;
    fixture.detectChanges();
  });

  it('recorta nombre y envía solo nombre; resultado sin permisos', () => {
    dialog.name.setValue(' Operaciones ');
    dialog.save();
    expect(create.execute).toHaveBeenCalledWith('Operaciones');
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(created.status).toBe('ACTIVO');
    expect(created.permissionCount).toBe(0);
  });

  it('rechaza nombre vacío o mayor a 80 sin invocar API', () => {
    dialog.name.setValue('   '); dialog.save();
    expect(create.execute).not.toHaveBeenCalled();
    dialog.name.setValue('x'.repeat(81)); dialog.save();
    expect(create.execute).not.toHaveBeenCalled();
  });

  it('muestra error backend sin cerrar dialog', () => {
    create.execute.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'Ya existe.' } })));
    dialog.name.setValue('Operaciones'); dialog.save();
    expect(dialog.error()).toBe('Ya existe.');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
