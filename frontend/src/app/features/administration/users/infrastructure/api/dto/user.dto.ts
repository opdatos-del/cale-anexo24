import { UserStatus } from '../../../domain/models/user-administration.model';

/** Usuario según el contrato HTTP V1. */
export interface UserResponseDto {
  id: number;
  clave: string;
  nombre: string;
  correo: string;
  estado: UserStatus;
  vigencia: string | null;
  perfilId: number;
  perfilNombre: string;
}

/** Página según el contrato HTTP V1. */
export interface UserPageResponseDto {
  items: UserResponseDto[];
  total: number;
  pagina: number;
  tamano: number;
}

export interface UpdateUserRequestDto {
  nombre: string;
  correo: string;
}

export interface ChangeUserStatusRequestDto {
  estado: UserStatus;
}

export interface ChangeUserExpirationRequestDto {
  vigencia: string | null;
}

export interface ResetUserPasswordRequestDto {
  password: string;
}
