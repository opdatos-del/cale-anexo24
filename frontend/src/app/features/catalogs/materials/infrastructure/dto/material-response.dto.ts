export interface MaterialResponseDto {
  materialkey: number;
  clave: string;
  descripcion: string;
  fraccion: string;
  unidad: string;
  unidadt: string;
  tipomaterial: string;
}

export interface MaterialPageResponseDto {
  items: MaterialResponseDto[];
  total: number;
  pagina: number;
  tamano: number;
}
