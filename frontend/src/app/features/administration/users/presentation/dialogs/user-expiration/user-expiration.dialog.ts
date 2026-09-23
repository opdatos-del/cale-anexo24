import { Component, OnDestroy, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { userFacingApiError } from '@core/http/api-error.util';
import { NotificationService } from '@core/notifications/notification.service';
import { ChangeUserExpirationUseCase } from '../../../application/use-cases/change-user-expiration.use-case';
import { UserAdministration } from '../../../domain/models/user-administration.model';

/** Convierte una fecha local a la fecha civil esperada por el backend. */
export function toLocalDate(value: Date | null): string | null {
  if (!value || Number.isNaN(value.getTime())) return null;
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function fromLocalDate(value: string | null): Date | null {
  if (!value) return null;
  const [year, month, day] = value.split('-').map(Number);
  return year && month && day ? new Date(year, month - 1, day) : null;
}

/** Cambia la vigencia o establece una cuenta sin vencimiento. */
@Component({
  selector: 'app-user-expiration-dialog',
  imports: [MatButtonModule, MatDatepickerModule, MatDialogModule, MatFormFieldModule, MatInputModule, ReactiveFormsModule],
  providers: [provideNativeDateAdapter(), { provide: MAT_DATE_LOCALE, useValue: 'es-MX' }],
  template: `
    <h2 mat-dialog-title>Cambiar vigencia</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content class="grid min-w-0 gap-3 sm:min-w-96">
        <p class="m-0 text-sm text-slate-500">{{ data.name }} ({{ data.key }})</p>
        <mat-form-field appearance="outline">
          <mat-label>Fecha de vigencia</mat-label>
          <input matInput [matDatepicker]="picker" formControlName="expiration" />
          <mat-datepicker-toggle matIconSuffix [for]="picker"></mat-datepicker-toggle>
          <mat-datepicker #picker></mat-datepicker>
          <mat-hint>Déjala vacía para una cuenta sin vencimiento.</mat-hint>
        </mat-form-field>
        <p class="m-0 text-xs text-amber-700">Una fecha anterior a hoy puede impedir el acceso del usuario.</p>
        @if (error()) { <p class="m-0 text-sm text-red-700" role="alert">{{ error() }}</p> }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" (click)="close()" [disabled]="saving()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="saving()">{{ saving() ? 'Guardando…' : 'Guardar' }}</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class UserExpirationDialog implements OnDestroy {
  protected readonly data = inject<UserAdministration>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UserExpirationDialog, UserAdministration | undefined>);
  private readonly changeExpiration = inject(ChangeUserExpirationUseCase);
  private readonly notifications = inject(NotificationService);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = new FormGroup({
    expiration: new FormControl<Date | null>(fromLocalDate(this.data.expiration)),
  });

  protected save(): void {
    this.saving.set(true);
    this.error.set(null);
    this.changeExpiration.execute(this.data.id, { expiration: toLocalDate(this.form.controls.expiration.value) }).subscribe({
      next: (user) => {
        this.notifications.success('Vigencia actualizada.');
        this.clearForm();
        this.dialogRef.close(user);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.error.set(userFacingApiError(error, 'No fue posible cambiar la vigencia.'));
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
    this.form.reset({ expiration: null });
  }
}
