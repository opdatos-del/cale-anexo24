import { Component, OnDestroy, inject, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { userFacingApiError } from '@core/http/api-error.util';
import { NotificationService } from '@core/notifications/notification.service';
import { ResetUserPasswordUseCase } from '../../../application/use-cases/reset-user-password.use-case';
import { UserAdministration } from '../../../domain/models/user-administration.model';

export const STRONG_PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{10,50}$/;

/** Valida que contraseña y confirmación sean idénticas. */
export const passwordsMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password')?.value as string | undefined;
  const confirmation = control.get('confirmation')?.value as string | undefined;
  return password === confirmation ? null : { passwordsMismatch: true };
};

/** Restablece una contraseña aplicando la política de seguridad. */
@Component({
  selector: 'app-user-password-dialog',
  imports: [MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule, ReactiveFormsModule],
  template: `
    <h2 mat-dialog-title>Restablecer contraseña</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content class="grid min-w-0 gap-3 sm:min-w-100">
        <p class="m-0 text-sm text-slate-500">{{ data.name }} ({{ data.key }})</p>
        <p class="m-0 text-xs text-slate-500">10 a 50 caracteres, con mayúscula, número y carácter especial.</p>
        <mat-form-field appearance="outline">
          <mat-label>Nueva contraseña</mat-label>
          <input matInput [type]="visible() ? 'text' : 'password'" formControlName="password" autocomplete="new-password" />
          <button mat-icon-button matIconSuffix type="button" (click)="visible.set(!visible())" [attr.aria-label]="visible() ? 'Ocultar contraseña' : 'Mostrar contraseña'">
            <mat-icon>{{ visible() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
          @if (form.controls.password.touched && form.controls.password.invalid) {
            <mat-error>La contraseña no cumple la política indicada.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Confirmar contraseña</mat-label>
          <input matInput [type]="visible() ? 'text' : 'password'" formControlName="confirmation" autocomplete="new-password" />
          @if (form.controls.confirmation.touched && form.hasError('passwordsMismatch')) {
            <mat-error>Las contraseñas no coinciden.</mat-error>
          }
        </mat-form-field>
        @if (error()) { <p class="m-0 text-sm text-red-700" role="alert">{{ error() }}</p> }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" (click)="close()" [disabled]="saving()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="saving()">{{ saving() ? 'Restableciendo…' : 'Restablecer' }}</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class UserPasswordDialog implements OnDestroy {
  protected readonly data = inject<UserAdministration>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UserPasswordDialog, boolean | undefined>);
  private readonly resetPassword = inject(ResetUserPasswordUseCase);
  private readonly notifications = inject(NotificationService);
  protected readonly saving = signal(false);
  protected readonly visible = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = new FormGroup(
    {
      password: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(STRONG_PASSWORD_PATTERN)] }),
      confirmation: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    },
    { validators: passwordsMatchValidator },
  );

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.resetPassword.execute(this.data.id, { password: this.form.controls.password.value }).subscribe({
      next: () => {
        this.notifications.success('Contraseña restablecida correctamente.');
        this.clearForm();
        this.dialogRef.close(true);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.error.set(userFacingApiError(error, 'No fue posible restablecer la contraseña.'));
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
    this.form.reset({ password: '', confirmation: '' });
  }
}
