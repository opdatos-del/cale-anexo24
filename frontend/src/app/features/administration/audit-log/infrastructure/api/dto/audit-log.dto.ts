import { AuditLogModule, AuditLogResult } from '@features/administration/audit-log/domain/models/audit-log.model';

/** Evento de Bitácora tal como lo devuelve el contrato HTTP V1. */
export interface AuditLogEntryResponseDto {
  id: number | string;
  fecha: string;
  usuarioId: number | null;
  usuario: string | null;
  modulo: AuditLogModule;
  accion: string;
  detalle: string | null;
  resultado: AuditLogResult;
  correlationId: string | null;
}

/** Página de eventos de Bitácora devuelta por el contrato HTTP V1. */
export interface AuditLogPageResponseDto {
  items: AuditLogEntryResponseDto[];
  total: number;
  pagina: number;
  tamano: number;
}
