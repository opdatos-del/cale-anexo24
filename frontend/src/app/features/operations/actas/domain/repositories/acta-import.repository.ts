import { Observable } from 'rxjs';
import { ActaConfirmation, ActaError, ActaLoad } from '@features/operations/actas/domain/models/acta-import.model';

export abstract class ActaImportRepository {
  abstract upload(file: File): Observable<ActaLoad>;
  abstract get(id: number, page: number, pageSize: number): Observable<ActaLoad>;
  abstract errors(id: number, page: number, pageSize: number): Observable<ActaError[]>;
  abstract confirm(id: number): Observable<ActaConfirmation>;
}
