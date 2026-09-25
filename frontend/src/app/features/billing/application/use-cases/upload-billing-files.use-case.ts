import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BillingUploadResponse } from '../../domain/models/billing-upload.model';
import { BillingRepository } from '../../domain/repositories/billing.repository';

@Injectable({ providedIn: 'root' })
export class UploadBillingFilesUseCase {
  private readonly repository = inject(BillingRepository);

  execute(files: File[]): Observable<BillingUploadResponse> {
    return this.repository.upload(files);
  }
}
