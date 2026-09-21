/** Criterios compartidos por las consultas paginadas de operaciones. */
export interface OperationSearchCriteria {
  from: string;
  to: string;
  customsDocument: string;
  customsCode: string;
  tariffFraction: string;
  partNumber: string;
  page: number;
  pageSize: number;
}
