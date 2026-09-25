/** Reportes disponibles en el contrato V1. */
export type ReportType = 'entradas' | 'salidas' | 'materiales-utilizados' | 'bitacora';

/** Filtros comunes y específicos admitidos por los reportes V1. */
export interface ReportSearchCriteria {
  type: ReportType;
  from: string;
  to: string;
  page: number;
  pageSize: number;
  customsDocument: string;
  customsCode: string;
  tariffFraction: string;
  partNumber: string;
  material: string;
  product: string;
  userId: number | null;
  module: string;
  result: string;
  correlationId: string;
}

/** Fila de reporte: las columnas dependen del reporte seleccionado. */
export type ReportRow = Record<string, string | number | null>;

/** Resultado paginado de un reporte. */
export interface ReportPage {
  items: ReportRow[];
  total: number;
  page: number;
  pageSize: number;
}
