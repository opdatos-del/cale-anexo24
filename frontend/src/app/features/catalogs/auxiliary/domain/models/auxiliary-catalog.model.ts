export type AuxiliaryCatalogKey = 'units' | 'materialTypes' | 'warehouses' | 'categories';

export interface AuxiliarySearchCriteria {
  filter: string;
  page: number;
  pageSize: number;
}

export interface AuxiliaryTableRow {
  id?: number;
  code?: string;
  name?: string;
  description?: string;
  alias?: string;
  validDays?: number;
  months?: number;
}

export interface AuxiliaryPage {
  items: AuxiliaryTableRow[];
  total: number;
  page: number;
  pageSize: number;
}

export interface UnidadResponseDto {
  clave: string;
  nombre: string;
  alias: string;
}

export interface TipoMaterialResponseDto {
  nombre: string;
}

export interface AlmacenResponseDto {
  almacenKey: number;
  clave: string;
  descripcion: string;
}

export interface CategoriaResponseDto {
  clave: string;
  descripcion: string;
  diasValidos: number;
  meses: number;
}

export interface AuxiliaryPageResponseDto<T> {
  items: T[];
  total: number;
  pagina: number;
  tamano: number;
}
