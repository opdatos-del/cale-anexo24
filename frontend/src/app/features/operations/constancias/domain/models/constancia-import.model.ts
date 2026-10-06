export interface ConstanciaError {
  hoja: string | null;
  fila: number | null;
  columna: string | null;
  valorEnmascarado: string | null;
  codigo: string;
  mensaje: string;
}

export type ConstanciaLoadState = 'PREVISUALIZADA' | 'CON_ERRORES' | 'CONFIRMADA';

/** Preview persistida de una carga de constancias (contrato CatalogImportResponse del backend). */
export interface ConstanciaLoad {
  id: number;
  tipo: string;
  archivo: string;
  hash: string;
  estado: ConstanciaLoadState;
  totalFilas: number;
  filasValidas: number;
  filasInvalidas: number;
  columnas: string[];
  filas: Record<string, string>[];
  totalPersistido: number;
  errores: ConstanciaError[];
}

/** Resultado de la confirmación autoritativa de una carga de constancias. */
export interface ConstanciaConfirmation {
  cargaId: number;
  estado: 'CONFIRMADA';
  totalFilas: number;
  filasValidas: number;
  filasConError: number;
  confirmadaEn: string | null;
  resultado: 'CONFIRMED';
}
