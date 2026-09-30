export type CatalogImportType = 'MATERIAL' | 'PRODUCTO';

export interface CatalogImportError {
  hoja: string | null;
  fila: number | null;
  columna: string | null;
  valorEnmascarado: string | null;
  codigo: string;
  mensaje: string;
}

export interface CatalogImportResponse {
  id: number;
  tipo: CatalogImportType;
  archivo: string;
  hash: string;
  estado: 'PREVISUALIZADA' | 'CON_ERRORES';
  totalFilas: number;
  filasValidas: number;
  filasInvalidas: number;
  columnas: string[];
  filas: Record<string, string>[];
  totalPersistido: number;
  errores: CatalogImportError[];
}
