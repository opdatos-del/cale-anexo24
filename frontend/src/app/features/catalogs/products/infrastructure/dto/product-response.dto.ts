export interface ProductResponseDto {
  id: string | number;
  clave: string;
  descripcion: string;
  fraccion: string;
  unidadComercial: string;
  unidadTarifaria: string;
}

export interface ProductPageResponseDto {
  items: ProductResponseDto[];
  total: number;
  pagina: number;
  tamano: number;
}
