export interface UsedMaterialResponseDto {
  descargaId: string | number;
  entradaId: string | number;
  partidaEntradaId: string | number;
  salidaId: string | number;
  partidaSalidaId: string | number;
  pedimentoEntrada: string | null;
  pedimentoSalida: string | null;
  materialCode: string | null;
  materialDescription: string | null;
  productCode: string | null;
  productDescription: string | null;
  cantidadIncorporada: number | string | null;
  cantidadMerma: number | string | null;
  cantidadDesperdicio: number | string | null;
  cantidadTotalDescargada: number | string | null;
  unidad: string | null;
  fecha: string | null;
}

export interface UsedMaterialPageResponseDto {
  items: UsedMaterialResponseDto[];
  total: number;
  pagina: number;
  tamano: number;
}
