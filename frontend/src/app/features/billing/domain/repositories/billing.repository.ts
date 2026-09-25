import { Observable } from 'rxjs';
import { BillingUploadResponse } from '@features/billing/domain/models/billing-upload.model';

export abstract class BillingRepository {
  abstract upload(files: File[]): Observable<BillingUploadResponse>;
}
