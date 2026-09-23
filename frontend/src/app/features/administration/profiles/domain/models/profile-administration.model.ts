/** Estados admitidos por la administración de perfiles. */
export type ProfileStatus = 'ACTIVO' | 'INACTIVO';

/** Perfil administrativo disponible para consulta. */
export interface ProfileAdministration {
  id: number;
  name: string;
  status: ProfileStatus;
  permissionCount: number;
}

/** Página de perfiles administrativos. */
export interface ProfilePage {
  items: ProfileAdministration[];
  total: number;
  page: number;
  pageSize: number;
}

/** Criterios de consulta paginada de perfiles. */
export interface ProfileSearchCriteria {
  name: string;
  status: ProfileStatus | null;
  page: number;
  pageSize: number;
}
