import { EntryLine } from '@features/operations/entries/domain/models/entry-line.model';
import { EntryPage } from '@features/operations/entries/domain/repositories/entry.repository';
import { EntryPageResponseDto, EntryResponseDto } from '@features/operations/entries/infrastructure/dto/entry-response.dto';

export const EntryMapper = {
  toDomain(dto: EntryResponseDto): EntryLine {
    return {
      importId: dto.importacionId,
      lineId: dto.partidaId,
      customsDocument: dto.pedimento,
      customsCode: dto.clavePedimento,
      entryDate: dto.fechaEntrada,
      tariffFraction: dto.fraccion,
      commercialUnit: dto.unidadComercial,
      quantity: dto.cantidadComercial,
      partNumber: dto.numeroParte,
      paymentDate: dto.fechaPago,
    };
  },

  toPage(dto: EntryPageResponseDto): EntryPage {
    return {
      items: dto.items.map((item) => EntryMapper.toDomain(item)),
      total: dto.total,
      page: dto.pagina,
      pageSize: dto.tamano,
    };
  },
};
