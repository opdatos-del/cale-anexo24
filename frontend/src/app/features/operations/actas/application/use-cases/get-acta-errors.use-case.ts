import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActaError } from '@features/operations/actas/domain/models/acta-import.model';
import { ActaImportRepository } from '@features/operations/actas/domain/repositories/acta-import.repository';

@Injectable({ providedIn: 'root' })
export class GetActaErrorsUseCase {
  private readonly repository = inject(ActaImportRepository);

  execute(cargaId: number, pagina: number, tamano: number): Observable<ActaError[]> {
    return this.repository.errors(cargaId, pagina, tamano);
  }
}
