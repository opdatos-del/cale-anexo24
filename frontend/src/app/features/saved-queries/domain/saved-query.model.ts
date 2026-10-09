export type SavedQueryScope = 'REPORTES' | 'ENTRADAS' | 'SALIDAS' | 'MATERIALES_UTILIZADOS' | 'ACTIVOS_FIJOS';
export type SavedCriteria = Record<string, string | number | null>;

export interface SavedQuery {
  id: number;
  nombre: string;
  descripcion: string | null;
  alcance: SavedQueryScope;
  criterios: SavedCriteria;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface SaveQueryRequest {
  nombre: string;
  descripcion: string | null;
  alcance: SavedQueryScope;
  criterios: SavedCriteria;
}
