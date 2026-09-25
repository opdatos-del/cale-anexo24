import { ProfileStatus } from '@features/administration/profiles/domain/models/profile-administration.model';

/** Actividad según el contrato HTTP. */
export interface ProfileActivityResponseDto {
  id: number;
  clave: string;
  nombre: string;
  recurso: string;
  accion: string;
}

/** Detalle de permisos según el contrato HTTP. */
export interface ProfilePermissionsResponseDto {
  perfilId: number;
  permisos: ProfileActivityResponseDto[];
}

/** Request HTTP para crear o renombrar perfil. */
export interface ProfileNameRequestDto {
  nombre: string;
}

/** Request HTTP para cambiar estado. */
export interface ProfileStatusRequestDto {
  estado: ProfileStatus;
}

/** Request HTTP de reemplazo completo de permisos. */
export interface ReplaceProfilePermissionsRequestDto {
  actividadIds: number[];
}
