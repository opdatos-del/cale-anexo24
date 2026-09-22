/** Módulos controlados de eventos de Bitácora. */
export type AuditLogModule =
  | 'SEGURIDAD'
  | 'CATALOGOS'
  | 'OPERACIONES'
  | 'ADMINISTRACION'
  | 'FACTURACION'
  | 'REPORTES'
  | 'SISTEMA';

/** Resultado controlado de un evento de Bitácora. */
export type AuditLogResult = 'EXITO' | 'FALLO';

/**
 * Acción de un evento.
 *
 * Cadena abierta: el backend puede ampliar el catálogo de acciones sin
 * romper el mapeo del frontend.
 */
export type AuditLogAction = string;

/** Evento de Bitácora ya persistido. */
export interface AuditLogEntry {
  id: number | string;
  /** Instante ISO-8601 en UTC devuelto por el backend. */
  date: string;
  userId: number | null;
  /** Etiqueta actual (no snapshot histórico) del usuario. */
  user: string | null;
  module: AuditLogModule;
  action: AuditLogAction;
  detail: string | null;
  result: AuditLogResult;
  correlationId: string | null;
}

/** Resultado paginado de la consulta de Bitácora. */
export interface AuditLogPage {
  items: AuditLogEntry[];
  total: number;
  page: number;
  pageSize: number;
}

/** Criterios de la consulta de Bitácora. */
export interface AuditLogSearchCriteria {
  /** Instante ISO-8601 con offset obligatorio. */
  from: string;
  /** Instante ISO-8601 con offset obligatorio. */
  to: string;
  userId: number | null;
  module: AuditLogModule | null;
  result: AuditLogResult | null;
  correlationId: string;
  page: number;
  pageSize: number;
}
