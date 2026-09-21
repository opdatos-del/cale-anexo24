export type TechnicalId = string | number;

export interface StructureLine {
  structureId: TechnicalId;
  productId: TechnicalId;
  productCode: string;
  productDescription: string;
  productUnit: string;
  startDate: string | null;
  endDate: string | null;
  productMaterialId: TechnicalId;
  materialCode: string;
  materialDescription: string;
  materialUnit: string;
  materialTariffFraction: string;
  incorporatedQuantity: number | string;
  wasteQuantity: number | string;
  scrapQuantity: number | string;
}
