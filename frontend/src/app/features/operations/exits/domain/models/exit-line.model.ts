/** Línea de salida para consulta operativa. */
export interface ExitLine {
  exitId: string | number;
  lineId: string | number;
  customsDocument: string | null;
  customsCode: string | null;
  tariffFraction: string | null;
  commercialUnit: string | null;
  quantity: number | string | null;
  partNumber: string | null;
  paymentDate: string | null;
}
