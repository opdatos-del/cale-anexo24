export interface FixedAssetResponseDto {
  partidaEntradaId: string | number;
  importacionId: string | number;
  pedimento: string | null;
  clavePedimento: string | null;
  fechaImportacion: string | null;
  numeroParte: string | null;
  descripcion: string | null;
  fraccion: string | null;
  cantidad: number | string | null;
  unidad: string | null;
  numeroSerie: string | null;
  marca: string | null;
  modelo: string | null;
}

export interface FixedAssetPageResponseDto {
  items: FixedAssetResponseDto[];
  total: number;
  pagina: number;
  tamano: number;
}
