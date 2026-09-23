/** Estados admitidos por la administración de usuarios. */
export type UserStatus = 'ACTIVO' | 'INACTIVO';

/** Usuario administrativo sin datos sensibles. */
export interface UserAdministration {
  id: number;
  key: string;
  name: string;
  email: string;
  status: UserStatus;
  expiration: string | null;
  profileId: number;
  profileName: string;
}

/** Página de usuarios. */
export interface UserPage {
  items: UserAdministration[];
  total: number;
  page: number;
  pageSize: number;
}

/** Criterios paginados de consulta. */
export interface UserSearchCriteria {
  key: string;
  name: string;
  email: string;
  status: UserStatus | null;
  profileId: number | null;
  page: number;
  pageSize: number;
}

/** Datos básicos editables. */
export interface UpdateUserCommand {
  name: string;
  email: string;
}

/** Cambio aislado de estado. */
export interface ChangeUserStatusCommand {
  status: UserStatus;
}

/** Cambio aislado de vigencia; null indica vigencia indefinida. */
export interface ChangeUserExpirationCommand {
  expiration: string | null;
}

/** Restablecimiento de contraseña. */
export interface ResetUserPasswordCommand {
  password: string;
}

/** Datos requeridos para crear un usuario administrativo. */
export interface CreateUserCommand {
  key: string;
  name: string;
  email: string;
  password: string;
  expiration: string | null;
  profileId: number;
}

/** Cambio aislado de perfil. */
export interface ChangeUserProfileCommand {
  profileId: number;
}
