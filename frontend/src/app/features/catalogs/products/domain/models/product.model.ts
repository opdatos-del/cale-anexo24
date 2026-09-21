export type TechnicalId = string | number;

export interface Product {
  id: TechnicalId;
  partNumber: string;
  description: string;
  tariffFraction: string;
  commercialUnit: string;
  tariffUnit: string;
}
