import { StructureLine } from '../../domain/models/structure-line.model';
import { StructurePage } from '../../domain/repositories/structure.repository';
import { StructurePageResponseDto, StructureResponseDto } from '../dto/structure-response.dto';

export const StructureMapper = {
  toDomain(dto: StructureResponseDto): StructureLine {
    return {
      structureId: dto.estructuraId,
      productId: dto.productoId,
      productCode: dto.productoClave,
      productDescription: dto.productoDescripcion,
      productUnit: dto.productoUnidad,
      startDate: dto.fechaInicio,
      endDate: dto.fechaFin,
      productMaterialId: dto.productoMaterialId,
      materialCode: dto.materialClave,
      materialDescription: dto.materialDescripcion,
      materialUnit: dto.materialUnidad,
      materialTariffFraction: dto.materialFraccion,
      incorporatedQuantity: dto.cantidadIncorporada,
      wasteQuantity: dto.cantidadMermada,
      scrapQuantity: dto.cantidadDesperdiciada,
    };
  },
  toPage(dto: StructurePageResponseDto): StructurePage {
    return {
      items: dto.items.map((item) => StructureMapper.toDomain(item)),
      total: dto.total,
      page: dto.pagina,
      pageSize: dto.tamano,
    };
  },
};
