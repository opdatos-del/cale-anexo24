import { Component, DestroyRef, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { userFacingApiError } from '@core/http/api-error.util';
import { NotificationService } from '@core/notifications/notification.service';
import { LoadAllProfilesUseCase } from '@features/administration/profiles/application/use-cases/load-all-profiles.use-case';
import { ProfileAdministration } from '@features/administration/profiles/domain/models/profile-administration.model';
import { CreateUserUseCase } from '@features/administration/users/application/use-cases/create-user.use-case';
import { UserAdministration } from '@features/administration/users/domain/models/user-administration.model';
import { passwordsMatchValidator, STRONG_PASSWORD_PATTERN } from '@features/administration/users/domain/validation/password.validation';
import { toLocalDate } from '../user-expiration/user-expiration.dialog';


/** Crea un usuario con un perfil activo y sin conservar su contraseña. */
@Component({
  selector: 'app-user-create-dialog',
  imports: [MatButtonModule, MatDatepickerModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, ReactiveFormsModule],
  providers: [provideNativeDateAdapter(), { provide: MAT_DATE_LOCALE, useValue: 'es-MX' }],
  template: `
    <h2 mat-dialog-title>Crear usuario</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content class="grid min-w-0 gap-3 sm:min-w-100">
        <mat-form-field appearance="outline">
          <mat-label>Clave</mat-label>
          <input matInput formControlName="key" maxlength="30" autocomplete="username" />
          @if (form.controls.key.touched && form.controls.key.invalid) {
            <mat-error>Ingresa una clave de hasta 30 caracteres.</mat-error>
          }
        </mat-form-field>
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
        <mat-form-field appearance="outline">
          <mat-label>Contraseña</mat-label>
          <input matInput type="password" formControlName="password" autocomplete="new-password" />
          @if (form.controls.password.touched && form.controls.password.invalid) {
            <mat-error>La contraseña no cumple la política indicada.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Confirmar contraseña</mat-label>
          <input matInput type="password" formControlName="confirmation" autocomplete="new-password" />
          @if (form.controls.confirmation.touched && form.hasError('passwordsMismatch')) {
            <mat-error>Las contraseñas no coinciden.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Perfil</mat-label>
          <mat-select formControlName="profileId" [attr.aria-busy]="loadingProfiles()">
            @for (profile of profiles(); track profile.id) {
              <mat-option [value]="profile.id">{{ profile.name }}</mat-option>
            }
          </mat-select>
          @if (form.controls.profileId.touched && form.controls.profileId.invalid) {
            <mat-error>Selecciona un perfil activo.</mat-error>
          }
        </mat-form-field>
        @if (loadingProfiles()) { <p class="m-0 text-sm text-slate-500" aria-live="polite">Cargando perfiles…</p> }
        @if (!loadingProfiles() && profiles().length === 0) { <p class="m-0 text-sm text-amber-700" role="alert">No hay perfiles activos disponibles.</p> }
        <mat-form-field appearance="outline">
          <mat-label>Vigencia</mat-label>
          <input matInput [matDatepicker]="picker" formControlName="expiration" />
          <mat-datepicker-toggle matIconSuffix [for]="picker"></mat-datepicker-toggle>
          <mat-datepicker #picker></mat-datepicker>
          <mat-hint>Opcional. Se guarda como fecha local.</mat-hint>
          @if (form.controls.expiration.touched && form.controls.expiration.invalid) {
            <mat-error>Ingresa una fecha válida.</mat-error>
          }
        </mat-form-field>
        @if (error()) { <p class="m-0 text-sm text-red-700" role="alert">{{ error() }}</p> }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" (click)="close()" [disabled]="saving()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="saving() || loadingProfiles() || profiles().length === 0">{{ saving() ? 'Creando…' : 'Crear' }}</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class UserCreateDialog implements OnInit, OnDestroy {
  private readonly dialogRef = inject(MatDialogRef<UserCreateDialog, UserAdministration | undefined>);
  private readonly createUser = inject(CreateUserUseCase);
  private readonly loadAllProfiles = inject(LoadAllProfilesUseCase);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly profiles = signal<ProfileAdministration[]>([]);
  protected readonly loadingProfiles = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = new FormGroup(
    {
      key: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(30)] }),
      name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
      email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email, Validators.maxLength(150)] }),
      password: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(STRONG_PASSWORD_PATTERN)] }),
      confirmation: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      profileId: new FormControl<number | null>(null, { validators: [Validators.required] }),
      expiration: new FormControl<Date | null>(null),
    },
    { validators: passwordsMatchValidator },
  );

  ngOnInit(): void {
    this.loadAllProfiles.execute('ACTIVO').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (profiles) => {
        this.profiles.set(profiles);
        this.loadingProfiles.set(false);
      },
      error: (error: unknown) => {
        this.loadingProfiles.set(false);
        this.error.set(userFacingApiError(error, 'No fue posible cargar los perfiles activos.'));
      },
    });
  }

  protected save(): void {
    if (this.saving()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.createUser.execute({
      key: value.key.trim(),
      name: value.name.trim(),
      email: value.email.trim(),
      password: value.password,
      profileId: value.profileId as number,
      expiration: toLocalDate(value.expiration),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (user) => {
        this.notifications.success('Usuario creado correctamente.');
        this.clearForm();
        this.dialogRef.close(user);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.error.set(userFacingApiError(error, 'No fue posible crear el usuario.'));
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
    this.form.reset({ key: '', name: '', email: '', password: '', confirmation: '', profileId: null, expiration: null });
  }
}
