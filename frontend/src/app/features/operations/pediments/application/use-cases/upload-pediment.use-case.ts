import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PedimentLoad } from '@features/operations/pediments/domain/models/pediment-upload.model';
import { PedimentRepository } from '@features/operations/pediments/domain/repositories/pediment.repository';

@Injectable({ providedIn: 'root' })
export class UploadPedimentUseCase {
  private readonly repository = inject(PedimentRepository);

  execute(file: File): Observable<PedimentLoad> {
    return this.repository.upload(file);
  }
}
