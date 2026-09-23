import { Component, OnDestroy, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { userFacingApiError } from '@core/http/api-error.util';
import { NotificationService } from '@core/notifications/notification.service';
import { UpdateUserUseCase } from '../../../application/use-cases/update-user.use-case';
import { UserAdministration } from '../../../domain/models/user-administration.model';

/** Edita exclusivamente nombre y correo. */
@Component({
  selector: 'app-user-edit-dialog',
  imports: [MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, ReactiveFormsModule],
  template: `
    <h2 mat-dialog-title>Editar usuario</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content class="grid min-w-0 gap-3 sm:min-w-100">
        <p class="m-0 text-sm text-slate-500">{{ data.key }} · {{ data.profileName }}</p>
        <mat-form-field appearance="outline">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="name" maxlength="120" autocomplete="name" />
          @if (form.controls.name.touched && form.controls.name.invalid) {
            <mat-error>Ingresa un nombre de hasta 120 caracteres.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Correo</mat-label>
          <input matInput type="email" formControlName="email" maxlength="150" autocomplete="email" />
          @if (form.controls.email.touched && form.controls.email.invalid) {
            <mat-error>Ingresa un correo válido.</mat-error>
          }
        </mat-form-field>
        @if (error()) { <p class="m-0 text-sm text-red-700" role="alert">{{ error() }}</p> }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" (click)="close()" [disabled]="saving()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="saving()">{{ saving() ? 'Guardando…' : 'Guardar' }}</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class UserEditDialog implements OnDestroy {
  protected readonly data = inject<UserAdministration>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UserEditDialog, UserAdministration | undefined>);
  private readonly updateUser = inject(UpdateUserUseCase);
  private readonly notifications = inject(NotificationService);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = new FormGroup({
    name: new FormControl(this.data.name, { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
    email: new FormControl(this.data.email, { nonNullable: true, validators: [Validators.required, Validators.email, Validators.maxLength(150)] }),
  });

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    const value = this.form.getRawValue();
    this.updateUser.execute(this.data.id, { name: value.name.trim(), email: value.email.trim() }).subscribe({
      next: (user) => {
        this.notifications.success('Usuario actualizado.');
        this.clearForm();
        this.dialogRef.close(user);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.error.set(userFacingApiError(error, 'No fue posible actualizar el usuario.'));
      },
    });
  }

  protected close(): void {
    this.clearForm();
    this.dialogRef.close();
  }

  ngOnDestroy(): void {
    this.clearForm();
  }

  private clearForm(): void {
    this.form.reset({ name: '', email: '' });
  }
}
