import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BillingUploadResponse } from '../../domain/models/billing-upload.model';

@Injectable({ providedIn: 'root' })
export class BillingApiService {
  private readonly http = inject(HttpClient);

  upload(files: File[]): Observable<BillingUploadResponse> {
    const form = new FormData();
    files.forEach((file) => form.append('archivos', file, file.name));
    return this.http.post<BillingUploadResponse>('/api/v1/facturacion/cargas', form);
  }
}
