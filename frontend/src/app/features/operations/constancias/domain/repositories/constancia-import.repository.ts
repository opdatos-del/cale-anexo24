import { Observable } from 'rxjs';
import { ConstanciaConfirmation, ConstanciaError, ConstanciaLoad } from '@features/operations/constancias/domain/models/constancia-import.model';

export abstract class ConstanciaImportRepository {
  abstract upload(file: File): Observable<ConstanciaLoad>;
  abstract get(id: number, page: number, pageSize: number): Observable<ConstanciaLoad>;
  abstract errors(id: number, page: number, pageSize: number): Observable<ConstanciaError[]>;
  abstract confirm(id: number): Observable<ConstanciaConfirmation>;
}
