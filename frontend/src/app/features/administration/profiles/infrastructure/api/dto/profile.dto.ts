import { ProfileStatus } from '@features/administration/profiles/domain/models/profile-administration.model';

/** Perfil según el contrato HTTP V1. */
export interface ProfileResponseDto {
  id: number;
  nombre: string;
  estado: ProfileStatus;
  cantidadPermisos: number;
}

/** Página de perfiles según el contrato HTTP V1. */
export interface ProfilePageResponseDto {
  items: ProfileResponseDto[];
  total: number;
  pagina: number;
  tamano: number;
}
