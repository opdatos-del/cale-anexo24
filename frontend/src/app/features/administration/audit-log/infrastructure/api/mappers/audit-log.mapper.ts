import { AuditLogEntry, AuditLogPage } from '@features/administration/audit-log/domain/models/audit-log.model';
import { AuditLogPageResponseDto, AuditLogEntryResponseDto } from '@features/administration/audit-log/infrastructure/api/dto/audit-log.dto';

/** Mapea el contrato HTTP de Bitácora al dominio frontend. */
export const AuditLogMapper = {
  toDomain(dto: AuditLogEntryResponseDto): AuditLogEntry {
    return {
      id: dto.id,
      date: dto.fecha,
      userId: dto.usuarioId,
      user: dto.usuario,
      module: dto.modulo,
      action: dto.accion,
      detail: dto.detalle,
      result: dto.resultado,
      correlationId: dto.correlationId,
    };
  },

  toPage(dto: AuditLogPageResponseDto): AuditLogPage {
    return {
      items: dto.items.map((item) => AuditLogMapper.toDomain(item)),
      total: dto.total,
      page: dto.pagina,
      pageSize: dto.tamano,
    };
  },
};
