import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BillingLoadDetail, BillingLoadSummary, BillingPage, BillingPersistedLoadStatus, BillingTemplate, BillingUploadResponse } from '@features/billing/domain/models/billing-upload.model';

@Injectable({ providedIn: 'root' })
export class BillingApiService {
  private readonly http = inject(HttpClient);

  template(): Observable<BillingTemplate> {
    return this.http.get<BillingTemplate>('/api/v1/facturacion/plantilla');
  }

  downloadTemplate(): Observable<Blob> {
    return this.http.get('/api/v1/facturacion/plantilla/archivo', { responseType: 'blob' });
  }

  load(id: number, pagina = 1, tamano = 100): Observable<BillingLoadDetail> {
    return this.http.get<BillingLoadDetail>('/api/v1/facturacion/cargas/' + id, { params: { pagina, tamano } });
  }

  history(estado: BillingPersistedLoadStatus | null, desde: string | null, hasta: string | null, pagina = 1, tamano = 20): Observable<BillingPage<BillingLoadSummary>> {
    let params = new HttpParams().set('pagina', pagina).set('tamano', tamano);
    if (estado) params = params.set('estado', estado);
    if (desde && hasta) params = params.set('desde', desde).set('hasta', hasta);
    return this.http.get<BillingPage<BillingLoadSummary>>('/api/v1/facturacion/cargas', { params });
  }

  upload(files: File[]): Observable<BillingUploadResponse> {
    const form = new FormData();
    files.forEach((file) => form.append('archivos', file, file.name));
    return this.http.post<BillingUploadResponse>('/api/v1/facturacion/cargas', form);
  }
}
