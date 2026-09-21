export interface ExitResponseDto {
  salidaId: string | number;
  partidaId: string | number;
  pedimento: string | null;
  clavePedimento: string | null;
  fraccion: string | null;
  unidadComercial: string | null;
  cantidad: number | string | null;
  numeroParte: string | null;
  fechaPago: string | null;
}

export interface ExitPageResponseDto {
  items: ExitResponseDto[];
  total: number;
  pagina: number;
  tamano: number;
}
