import {
  AlmacenResponseDto,
  AuxiliaryPage,
  AuxiliaryPageResponseDto,
  CategoriaResponseDto,
  TipoMaterialResponseDto,
  UnidadResponseDto,
} from '@features/catalogs/auxiliary/domain/models/auxiliary-catalog.model';

/** Traduce cada contrato REST auxiliar al modelo de presentación de la tabla. */
export const AuxiliaryCatalogMapper = {
  units(response: AuxiliaryPageResponseDto<UnidadResponseDto>): AuxiliaryPage {
    return page(response, response.items.map((item) => ({ code: item.clave, name: item.nombre, alias: item.alias })));
  },
  materialTypes(response: AuxiliaryPageResponseDto<TipoMaterialResponseDto>): AuxiliaryPage {
    return page(response, response.items.map((item) => ({ name: item.nombre })));
  },
  warehouses(response: AuxiliaryPageResponseDto<AlmacenResponseDto>): AuxiliaryPage {
    return page(response, response.items.map((item) => ({ id: item.almacenKey, code: item.clave, description: item.descripcion })));
  },
  categories(response: AuxiliaryPageResponseDto<CategoriaResponseDto>): AuxiliaryPage {
    return page(response, response.items.map((item) => ({ code: item.clave, description: item.descripcion, validDays: item.diasValidos, months: item.meses })));
  },
};

function page(response: { total: number; pagina: number; tamano: number }, items: AuxiliaryPage['items']): AuxiliaryPage {
  return { items, total: response.total, page: response.pagina, pageSize: response.tamano };
}
