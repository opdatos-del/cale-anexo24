import { FixedAsset, FixedAssetPage } from '@features/operations/fixed-assets/domain/models/fixed-asset.model';
import { FixedAssetPageResponseDto, FixedAssetResponseDto } from '@features/operations/fixed-assets/infrastructure/api/dto/fixed-asset.dto';

/** Mapea el contrato HTTP de activos fijos al dominio frontend. */
export const FixedAssetMapper = {
  toDomain(dto: FixedAssetResponseDto): FixedAsset {
    return {
      entryLineId: dto.partidaEntradaId,
      importId: dto.importacionId,
      customsDocument: dto.pedimento,
      customsCode: dto.clavePedimento,
      importDate: dto.fechaImportacion,
      partNumber: dto.numeroParte,
      description: dto.descripcion,
      tariffFraction: dto.fraccion,
      quantity: dto.cantidad,
      unit: dto.unidad,
      serialNumber: dto.numeroSerie,
      brand: dto.marca,
      model: dto.modelo,
    };
  },

  toPage(dto: FixedAssetPageResponseDto): FixedAssetPage {
    return {
      items: dto.items.map((item) => FixedAssetMapper.toDomain(item)),
      total: dto.total,
      page: dto.pagina,
      pageSize: dto.tamano,
    };
  },
};
