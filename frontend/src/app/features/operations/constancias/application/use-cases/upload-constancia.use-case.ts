import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConstanciaLoad } from '@features/operations/constancias/domain/models/constancia-import.model';
import { ConstanciaImportRepository } from '@features/operations/constancias/domain/repositories/constancia-import.repository';

/** Valida y guarda en staging una carga de constancias previsualizada. */
@Injectable({ providedIn: 'root' })
export class UploadConstanciaUseCase {
  private readonly repository = inject(ConstanciaImportRepository);

  execute(file: File): Observable<ConstanciaLoad> {
    return this.repository.upload(file);
  }
}
