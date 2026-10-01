import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PedimentLoad } from '@features/operations/pediments/domain/models/pediment-upload.model';
import { PedimentRepository } from '@features/operations/pediments/domain/repositories/pediment.repository';

/** Recupera una carga persistida para refrescar su estado real. */
@Injectable({ providedIn: 'root' })
export class GetPedimentUseCase {
  private readonly repository = inject(PedimentRepository);

  execute(id: number, page = 1, pageSize = 100): Observable<PedimentLoad> {
    return this.repository.get(id, page, pageSize);
  }
}
