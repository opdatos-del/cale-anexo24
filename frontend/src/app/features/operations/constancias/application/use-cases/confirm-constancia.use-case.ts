import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConstanciaConfirmation } from '@features/operations/constancias/domain/models/constancia-import.model';
import { ConstanciaImportRepository } from '@features/operations/constancias/domain/repositories/constancia-import.repository';

/** Confirma de forma autoritativa una carga de constancias previsualizada. */
@Injectable({ providedIn: 'root' })
export class ConfirmConstanciaUseCase {
  private readonly repository = inject(ConstanciaImportRepository);

  execute(id: number): Observable<ConstanciaConfirmation> {
    return this.repository.confirm(id);
  }
}
