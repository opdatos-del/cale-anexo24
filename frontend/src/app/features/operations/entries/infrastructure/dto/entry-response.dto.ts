export interface EntryResponseDto {
  importacionId: string | number;
  partidaId: string | number;
  pedimento: string | null;
  clavePedimento: string | null;
  fechaEntrada: string | null;
  fraccion: string | null;
  unidadComercial: string | null;
  cantidadComercial: number | string | null;
  numeroParte: string | null;
  fechaPago: string | null;
}

export interface EntryPageResponseDto {
  items: EntryResponseDto[];
  total: number;
  pagina: number;
  tamano: number;
}
