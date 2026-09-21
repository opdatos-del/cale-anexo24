import { Product } from '../../domain/models/product.model';
import { ProductPage } from '../../domain/repositories/product.repository';
import { ProductPageResponseDto, ProductResponseDto } from '../dto/product-response.dto';

export const ProductMapper = {
  toDomain(dto: ProductResponseDto): Product {
    return {
      id: dto.id,
      partNumber: dto.clave,
      description: dto.descripcion,
      tariffFraction: dto.fraccion,
      commercialUnit: dto.unidadComercial,
      tariffUnit: dto.unidadTarifaria,
    };
  },
  toPage(dto: ProductPageResponseDto): ProductPage {
    return {
      items: dto.items.map((item) => ProductMapper.toDomain(item)),
      total: dto.total,
      page: dto.pagina,
      pageSize: dto.tamano,
    };
  },
};
