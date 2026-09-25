import { ReportPage, ReportRow } from '../../domain/models/report.model';

interface ReportPageResponseDto {
  items: ReportRow[];
  total: number;
  pagina: number;
  tamano: number;
}

/** Mapea la respuesta paginada del contrato HTTP de Reportes. */
export const ReportMapper = {
  toPage(dto: ReportPageResponseDto): ReportPage {
    return { items: dto.items, total: dto.total, page: dto.pagina, pageSize: dto.tamano };
  },
};
