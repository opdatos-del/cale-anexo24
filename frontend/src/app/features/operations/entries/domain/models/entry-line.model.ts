/** Línea de entrada para consulta operativa. */
export interface EntryLine {
  importId: string | number;
  lineId: string | number;
  customsDocument: string | null;
  customsCode: string | null;
  entryDate: string | null;
  tariffFraction: string | null;
  commercialUnit: string | null;
  quantity: number | string | null;
  partNumber: string | null;
  paymentDate: string | null;
}
