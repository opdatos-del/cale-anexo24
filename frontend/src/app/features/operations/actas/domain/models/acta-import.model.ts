export interface ActaError {
  hoja: string | null;
  fila: number | null;
  columna: string | null;
  valorEnmascarado: string | null;
  codigo: string;
  mensaje: string;
}

export type ActaLoadState = 'PREVISUALIZADA' | 'CON_ERRORES' | 'CONFIRMADA';

/** Preview persistida de una carga de actas (contrato CatalogImportResponse del backend). */
export interface ActaLoad {
  id: number;
  tipo: string;
  archivo: string;
  hash: string;
  estado: ActaLoadState;
  totalFilas: number;
  filasValidas: number;
  filasInvalidas: number;
  columnas: string[];
  filas: Record<string, string>[];
  totalPersistido: number;
  errores: ActaError[];
}

/** Resultado de la confirmación autoritativa de una carga de actas. */
export interface ActaConfirmation {
  cargaId: number;
  estado: 'CONFIRMADA';
  totalFilas: number;
  filasValidas: number;
  filasConError: number;
  confirmadaEn: string | null;
  resultado: 'CONFIRMED';
}
