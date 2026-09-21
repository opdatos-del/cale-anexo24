export interface StructureResponseDto {
  estructuraId: string | number;
  productoId: string | number;
  productoClave: string;
  productoDescripcion: string;
  productoUnidad: string;
  fechaInicio: string | null;
  fechaFin: string | null;
  productoMaterialId: string | number;
  materialClave: string;
  materialDescripcion: string;
  materialUnidad: string;
  materialFraccion: string;
  cantidadIncorporada: number | string;
  cantidadMermada: number | string;
  cantidadDesperdiciada: number | string;
}

export interface StructurePageResponseDto {
  items: StructureResponseDto[];
  total: number;
  pagina: number;
  tamano: number;
}
