import { Component, DestroyRef, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { userFacingApiError } from '@core/http/api-error.util';
import { NotificationService } from '@core/notifications/notification.service';
import { LoadAllProfilesUseCase } from '@features/administration/profiles/application/use-cases/load-all-profiles.use-case';
import { ProfileAdministration } from '@features/administration/profiles/domain/models/profile-administration.model';
import { ChangeUserProfileUseCase } from '@features/administration/users/application/use-cases/change-user-profile.use-case';
import { UserAdministration } from '@features/administration/users/domain/models/user-administration.model';

/** Cambia el perfil de un usuario por uno que se encuentre activo. */
@Component({
  selector: 'app-user-profile-dialog',
  imports: [MatButtonModule, MatDialogModule, MatFormFieldModule, MatSelectModule, ReactiveFormsModule],
  template: `
    <h2 mat-dialog-title>Cambiar perfil</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content class="grid min-w-0 gap-3 sm:min-w-96">
        <p class="m-0 text-sm text-slate-500">{{ data.name }} ({{ data.key }})</p>
        @if (loadingProfiles()) { <p class="m-0 text-sm text-slate-500" aria-live="polite">Cargando perfiles…</p> }
        @if (!loadingProfiles() && currentProfileIsInactive()) {
          <p class="m-0 text-sm text-amber-700" role="status">Perfil actual: {{ data.profileName }} (Inactivo). Selecciona un perfil activo para continuar.</p>
        }
        <mat-form-field appearance="outline">
          <mat-label>Perfil activo</mat-label>
          <mat-select formControlName="profileId" [attr.aria-busy]="loadingProfiles()">
            @for (profile of profiles(); track profile.id) {
              <mat-option [value]="profile.id">{{ profile.name }}</mat-option>
            }
          </mat-select>
          @if (form.controls.profileId.touched && form.controls.profileId.invalid) {
            <mat-error>Selecciona un perfil activo.</mat-error>
          }
        </mat-form-field>
        @if (error()) { <p class="m-0 text-sm text-red-700" role="alert">{{ error() }}</p> }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" (click)="close()" [disabled]="saving()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="saving() || loadingProfiles() || form.controls.profileId.value === data.profileId">{{ saving() ? 'Guardando…' : 'Guardar' }}</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class UserProfileDialog implements OnInit, OnDestroy {
  protected readonly data = inject<UserAdministration>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UserProfileDialog, UserAdministration | undefined>);
  private readonly changeUserProfile = inject(ChangeUserProfileUseCase);
  private readonly loadAllProfiles = inject(LoadAllProfilesUseCase);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly profiles = signal<ProfileAdministration[]>([]);
  protected readonly loadingProfiles = signal(true);
  protected readonly currentProfileIsInactive = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = new FormGroup({
    profileId: new FormControl<number | null>(null, { validators: [Validators.required] }),
  });

  ngOnInit(): void {
    this.loadAllProfiles.execute('ACTIVO').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (profiles) => {
        this.profiles.set(profiles);
        const currentProfileIsActive = profiles.some((profile) => profile.id === this.data.profileId);
        this.currentProfileIsInactive.set(!currentProfileIsActive);
        this.form.controls.profileId.setValue(currentProfileIsActive ? this.data.profileId : null);
        this.loadingProfiles.set(false);
      },
      error: (error: unknown) => {
        this.loadingProfiles.set(false);
        this.error.set(userFacingApiError(error, 'No fue posible cargar los perfiles activos.'));
      },
    });
  }

  protected save(): void {
    if (this.saving() || this.form.controls.profileId.value === this.data.profileId) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.changeUserProfile.execute(this.data.id, { profileId: this.form.controls.profileId.value as number }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (user) => {
        this.notifications.success('Perfil actualizado correctamente.');
        this.clearForm();
        this.dialogRef.close(user);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.error.set(userFacingApiError(error, 'No fue posible cambiar el perfil.'));
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
    this.form.reset({ profileId: null });
  }
}
