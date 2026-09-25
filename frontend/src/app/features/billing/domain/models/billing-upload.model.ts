export type BillingLoadStatus = 'VALIDADA' | 'CON_ERRORES' | 'FALLIDA';

export interface BillingValidationError {
  hoja: string | null;
  fila: number | null;
  columna: string | null;
  valorEnmascarado: string | null;
  codigo: string;
  mensaje: string;
}

export interface BillingPreview {
  columnas: string[];
  filas: Record<string, string | null>[];
}

export interface BillingLoad {
  id: number;
  archivo: string;
  hash: string;
  estado: BillingLoadStatus;
  totalRegistros: number;
  registrosValidos: number;
  registrosInvalidos: number;
  preview: BillingPreview;
  errores: BillingValidationError[];
}

export interface BillingUploadResponse {
  cargas: BillingLoad[];
  correlationId: string;
  plantilla: 'FACTURACION_TEMPLATE_V1_PROVISIONAL';
  confirmacionDisponible: false;
}
