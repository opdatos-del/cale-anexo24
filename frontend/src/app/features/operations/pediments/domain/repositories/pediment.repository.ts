import { Observable } from 'rxjs';
import { PedimentLoad } from '@features/operations/pediments/domain/models/pediment-upload.model';

export abstract class PedimentRepository {
  abstract upload(file: File): Observable<PedimentLoad>;
  abstract get(id: number, page: number, pageSize: number): Observable<PedimentLoad>;
}
