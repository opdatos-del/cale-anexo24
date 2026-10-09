import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PedimentError } from '@features/operations/pediments/domain/models/pediment-upload.model';
import { PedimentRepository } from '@features/operations/pediments/domain/repositories/pediment.repository';

@Injectable({ providedIn: 'root' })
export class GetPedimentErrorsUseCase {
  private readonly repository = inject(PedimentRepository);

  execute(cargaId: number, pagina: number, tamano: number): Observable<PedimentError[]> {
    return this.repository.errors(cargaId, pagina, tamano);
  }
}
