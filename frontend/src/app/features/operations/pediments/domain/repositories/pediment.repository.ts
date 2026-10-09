import { Observable } from 'rxjs';
import { PedimentConfirmation, PedimentError, PedimentLoad } from '@features/operations/pediments/domain/models/pediment-upload.model';

export abstract class PedimentRepository {
  abstract upload(file: File): Observable<PedimentLoad>;
  abstract get(id: number, page: number, pageSize: number): Observable<PedimentLoad>;
  abstract errors(id: number, page: number, pageSize: number): Observable<PedimentError[]>;
  abstract confirm(id: number): Observable<PedimentConfirmation>;
}
