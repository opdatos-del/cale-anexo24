import { Observable } from 'rxjs';
import { BillingUploadResponse } from '../models/billing-upload.model';

export abstract class BillingRepository {
  abstract upload(files: File[]): Observable<BillingUploadResponse>;
}
