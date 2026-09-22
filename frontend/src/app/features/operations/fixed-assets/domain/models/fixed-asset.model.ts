/** Partida de importación marcada históricamente como activo fijo. */
export interface FixedAsset {
  entryLineId: string | number;
  importId: string | number;
  customsDocument: string | null;
  customsCode: string | null;
  importDate: string | null;
  partNumber: string | null;
  description: string | null;
  tariffFraction: string | null;
  quantity: number | string | null;
  unit: string | null;
  serialNumber: string | null;
  brand: string | null;
  model: string | null;
}

/** Resultado paginado de la consulta de activos fijos. */
export interface FixedAssetPage {
  items: FixedAsset[];
  total: number;
  page: number;
  pageSize: number;
}

/** Criterios propios de la consulta de activos fijos. */
export interface FixedAssetSearchCriteria {
  from: string | null;
  to: string | null;
  customsDocument: string;
  customsCode: string;
  partNumber: string;
  description: string;
  serialNumber: string;
  brand: string;
  model: string;
  page: number;
  pageSize: number;
}
