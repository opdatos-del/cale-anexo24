export type CatalogImportType = 'MATERIAL' | 'PRODUCTO' | 'CLIENTE' | 'PROVEEDOR';

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
  estado: 'PREVISUALIZADA' | 'CON_ERRORES' | 'CONFIRMADA';
  totalFilas: number;
  filasValidas: number;
  filasInvalidas: number;
  columnas: string[];
  filas: Record<string, string>[];
  totalPersistido: number;
  errores: CatalogImportError[];
}

export interface CatalogMaterialImportConfirmation {
  cargaId: number;
  estado: 'CONFIRMADA';
  totalFilas: number;
  filasValidas: number;
  filasConError: number;
  confirmadaEn: string | null;
}

export interface CatalogProductImportConfirmation {
  cargaId: number;
  estado: 'CONFIRMADA';
  totalFilas: number;
  filasValidas: number;
  filasConError: number;
  confirmadaEn: string | null;
}

export interface CatalogClientImportConfirmation {
  cargaId: number;
  estado: 'CONFIRMADA';
  totalFilas: number;
  filasValidas: number;
  filasConError: number;
  confirmadaEn: string | null;
}

export interface CatalogProviderImportConfirmation {
  cargaId: number;
  estado: 'CONFIRMADA';
  totalFilas: number;
  filasValidas: number;
  filasConError: number;
  confirmadaEn: string | null;
}
