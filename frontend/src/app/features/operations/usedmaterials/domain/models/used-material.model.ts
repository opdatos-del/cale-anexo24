/** Fila histórica de material utilizado en una salida. */
export interface UsedMaterial {
  dischargeId: string | number;
  entryId: string | number;
  entryLineId: string | number;
  exitId: string | number;
  exitLineId: string | number;
  entryCustomsDocument: string | null;
  exitCustomsDocument: string | null;
  materialCode: string | null;
  materialDescription: string | null;
  productCode: string | null;
  productDescription: string | null;
  incorporatedQuantity: number | string | null;
  wasteQuantity: number | string | null;
  scrapQuantity: number | string | null;
  totalDischargedQuantity: number | string | null;
  unit: string | null;
  date: string | null;
}

/** Resultado paginado de la consulta de materiales utilizados. */
export interface UsedMaterialPage {
  items: UsedMaterial[];
  total: number;
  page: number;
  pageSize: number;
}

/** Criterios de búsqueda propios del histórico de materiales utilizados. */
export interface UsedMaterialSearchCriteria {
  from: string;
  to: string;
  material: string;
  product: string;
  exitCustomsDocument: string;
  exitCustomsCode: string;
  page: number;
  pageSize: number;
}
