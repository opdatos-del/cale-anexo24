import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PedimentConfirmation } from '@features/operations/pediments/domain/models/pediment-upload.model';
import { PedimentRepository } from '@features/operations/pediments/domain/repositories/pediment.repository';

/** Confirma de forma autoritativa una carga previsualizada. */
@Injectable({ providedIn: 'root' })
export class ConfirmPedimentUseCase {
  private readonly repository = inject(PedimentRepository);

  execute(id: number): Observable<PedimentConfirmation> {
    return this.repository.confirm(id);
  }
}
