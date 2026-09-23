import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Política única de contraseña para creación y restablecimiento. */
export const STRONG_PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{10,50}$/;

/** Valida que contraseña y confirmación sean idénticas. */
export const passwordsMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password')?.value as string | undefined;
  const confirmation = control.get('confirmation')?.value as string | undefined;
  return password === confirmation ? null : { passwordsMismatch: true };
};
