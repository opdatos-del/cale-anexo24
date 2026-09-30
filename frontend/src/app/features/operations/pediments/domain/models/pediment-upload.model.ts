export interface PedimentError {
  hoja: string | null;
  fila: number | null;
  columna: string | null;
  valorEnmascarado: string | null;
  codigo: string;
  mensaje: string;
}

export interface PedimentPreview {
  columnas: string[];
  filas: Record<string, string>[];
  pagina: number;
  tamano: number;
  totalFilas: number;
}

export interface PedimentLoad {
  id: number;
  archivo: string;
  hash: string;
  estado: 'PREVISUALIZADA' | 'CON_ERRORES';
  totalFilas: number;
  filasValidas: number;
  filasInvalidas: number;
  versionPlantilla: string;
  correlationId: string;
  preview: PedimentPreview;
  errores: PedimentError[];
}
