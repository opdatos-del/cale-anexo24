import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BillingTemplate, BillingUploadResponse } from '@features/billing/domain/models/billing-upload.model';

@Injectable({ providedIn: 'root' })
export class BillingApiService {
  private readonly http = inject(HttpClient);

  template(): Observable<BillingTemplate> {
    return this.http.get<BillingTemplate>('/api/v1/facturacion/plantilla');
  }

  downloadTemplate(): Observable<Blob> {
    return this.http.get('/api/v1/facturacion/plantilla/archivo', { responseType: 'blob' });
  }

  load(id: number, pagina = 1, tamano = 100): Observable<unknown> {
    return this.http.get(`/api/v1/facturacion/cargas/${id}`, { params: { pagina, tamano } });
  }

  upload(files: File[]): Observable<BillingUploadResponse> {
    const form = new FormData();
    files.forEach((file) => form.append('archivos', file, file.name));
    return this.http.post<BillingUploadResponse>('/api/v1/facturacion/cargas', form);
  }
}
