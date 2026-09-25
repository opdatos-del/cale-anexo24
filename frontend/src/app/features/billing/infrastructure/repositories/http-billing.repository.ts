import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BillingUploadResponse } from '@features/billing/domain/models/billing-upload.model';
import { BillingRepository } from '@features/billing/domain/repositories/billing.repository';
import { BillingApiService } from '../api/billing-api.service';

@Injectable({ providedIn: 'root' })
export class HttpBillingRepository extends BillingRepository {
  private readonly api = inject(BillingApiService);

  upload(files: File[]): Observable<BillingUploadResponse> {
    return this.api.upload(files);
  }
}
