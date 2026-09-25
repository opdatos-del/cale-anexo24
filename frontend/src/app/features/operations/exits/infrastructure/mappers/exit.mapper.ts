import { ExitLine } from '@features/operations/exits/domain/models/exit-line.model';
import { ExitPage } from '@features/operations/exits/domain/repositories/exit.repository';
import { ExitPageResponseDto, ExitResponseDto } from '@features/operations/exits/infrastructure/dto/exit-response.dto';

export const ExitMapper = {
  toDomain(dto: ExitResponseDto): ExitLine {
    return {
      exitId: dto.salidaId,
      lineId: dto.partidaId,
      customsDocument: dto.pedimento,
      customsCode: dto.clavePedimento,
      tariffFraction: dto.fraccion,
      commercialUnit: dto.unidadComercial,
      quantity: dto.cantidad,
      partNumber: dto.numeroParte,
      paymentDate: dto.fechaPago,
    };
  },

  toPage(dto: ExitPageResponseDto): ExitPage {
    return {
      items: dto.items.map((item) => ExitMapper.toDomain(item)),
      total: dto.total,
      page: dto.pagina,
      pageSize: dto.tamano,
    };
  },
};
