import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConstanciaError } from '@features/operations/constancias/domain/models/constancia-import.model';
import { ConstanciaImportRepository } from '@features/operations/constancias/domain/repositories/constancia-import.repository';

@Injectable({ providedIn: 'root' })
export class GetConstanciaErrorsUseCase {
  private readonly repository = inject(ConstanciaImportRepository);

  execute(cargaId: number, pagina: number, tamano: number): Observable<ConstanciaError[]> {
    return this.repository.errors(cargaId, pagina, tamano);
  }
}
