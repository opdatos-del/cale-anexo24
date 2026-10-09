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

export type PedimentLoadState = 'PREVISUALIZADA' | 'CON_ERRORES' | 'CONFIRMADA';

export interface PedimentLoad {
  id: number;
  archivo: string;
  hash: string;
  estado: PedimentLoadState;
  totalFilas: number;
  filasValidas: number;
  filasInvalidas: number;
  versionPlantilla: string;
  correlationId: string;
  preview: PedimentPreview;
  errores: PedimentError[];
}

/** Resultado de la confirmación autoritativa de una carga. */
export interface PedimentConfirmation {
  cargaId: number;
  estado: 'CONFIRMADA';
  resultado: 'CONFIRMED' | 'ALREADY_CONFIRMED';
  tipoOperacion: number | null;
  operacionesProcesadas: number;
  partidasProcesadas: number;
  fechaConfirmacion: string;
}

export interface PedimentErrorsResponse {
  cargaId: number;
  pagina: number;
  tamano: number;
  errores: PedimentError[];
}
