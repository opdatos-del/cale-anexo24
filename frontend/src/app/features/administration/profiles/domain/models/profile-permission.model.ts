/** Actividad disponible para asignación a perfiles. */
export interface ProfileActivity {
  id: number;
  key: string;
  name: string;
  resource: string;
  action: string;
}

/** Conjunto completo de actividades asignadas a un perfil. */
export interface ProfilePermissions {
  profileId: number;
  permissions: ProfileActivity[];
}
