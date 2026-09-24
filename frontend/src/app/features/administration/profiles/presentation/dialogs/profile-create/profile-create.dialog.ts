import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { userFacingApiError } from '@core/http/api-error.util';
import { CreateProfileUseCase } from '../../../application/use-cases/create-profile.use-case';
import { ProfileAdministration } from '../../../domain/models/profile-administration.model';

/** Dialog de creación de perfil, sin asignación implícita de permisos. */
@Component({
  selector: 'app-profile-create-dialog',
  imports: [MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, ReactiveFormsModule],
  template: `
    <h2 mat-dialog-title>Crear perfil</h2>
    <form (ngSubmit)="save()">
      <mat-dialog-content class="grid gap-3">
        <p class="m-0 text-sm text-slate-600">El perfil se creará activo y sin permisos. Podrás asignarlos después.</p>
        <mat-form-field appearance="outline">
          <mat-label>Nombre del perfil</mat-label>
          <input matInput [formControl]="name" maxlength="80" autocomplete="off" aria-label="Nombre del perfil" />
          @if (name.touched && name.invalid) { <mat-error>Ingresa nombre de hasta 80 caracteres.</mat-error> }
        </mat-form-field>
        @if (error()) { <p class="m-0 text-sm text-red-700" role="alert">{{ error() }}</p> }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" (click)="dialog.close()" [disabled]="saving()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="saving()">{{ saving() ? 'Creando…' : 'Crear perfil' }}</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class ProfileCreateDialog {
  protected readonly name = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] });
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly dialog = inject(MatDialogRef<ProfileCreateDialog, ProfileAdministration | undefined>);
  private readonly createProfile = inject(CreateProfileUseCase);
  private readonly destroyRef = inject(DestroyRef);

  protected save(): void {
    const name = this.name.value.trim();
    if (this.saving()) return;
    if (!name || name.length > 80) {
      this.name.markAsTouched();
      this.name.setErrors({ ...(this.name.errors ?? {}), [name ? 'maxlength' : 'required']: true });
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.createProfile.execute(name).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (profile) => this.dialog.close(profile),
      error: (failure: unknown) => {
        this.saving.set(false);
        this.error.set(userFacingApiError(failure, 'No fue posible crear el perfil.'));
      },
    });
  }
}
