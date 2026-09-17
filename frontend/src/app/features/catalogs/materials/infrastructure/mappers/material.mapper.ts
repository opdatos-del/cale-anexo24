import { Material } from '../../domain/models/material.model';
import { MaterialPage } from '../../domain/repositories/material.repository';
import { MaterialPageResponseDto, MaterialResponseDto } from '../dto/material-response.dto';

export const MaterialMapper = {
  toDomain(dto: MaterialResponseDto): Material {
    return {
      materialKey: dto.materialkey,
      partNumber: dto.clave,
      description: dto.descripcion,
      tariffFraction: dto.fraccion,
      unit: dto.unidad,
      umc: dto.unidadt,
      materialType: dto.tipomaterial,
    };
  },
  toPage(dto: MaterialPageResponseDto): MaterialPage {
    return { items: dto.items.map((item) => MaterialMapper.toDomain(item)), total: dto.total, page: dto.pagina, pageSize: dto.tamano };
  },
};
