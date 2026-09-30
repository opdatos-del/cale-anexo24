import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PedimentLoad } from '@features/operations/pediments/domain/models/pediment-upload.model';
import { PedimentRepository } from '@features/operations/pediments/domain/repositories/pediment.repository';
import { PedimentApiService } from '@features/operations/pediments/infrastructure/api/pediment-api.service';

@Injectable({ providedIn: 'root' })
export class HttpPedimentRepository implements PedimentRepository {
  private readonly api = inject(PedimentApiService);

  upload(file: File): Observable<PedimentLoad> {
    return this.api.upload(file);
  }

  get(id: number, page: number, pageSize: number): Observable<PedimentLoad> {
    return this.api.get(id, page, pageSize);
  }
}
