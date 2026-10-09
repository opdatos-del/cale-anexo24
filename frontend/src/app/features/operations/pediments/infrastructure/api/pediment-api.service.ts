import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PedimentConfirmation, PedimentErrorsResponse, PedimentLoad } from '@features/operations/pediments/domain/models/pediment-upload.model';

@Injectable({ providedIn: 'root' })
export class PedimentApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/operaciones/pedimentos';

  upload(file: File): Observable<PedimentLoad> {
    const form = new FormData();
    form.append('archivo', file, file.name);
    return this.http.post<PedimentLoad>(`${this.baseUrl}/cargas`, form);
  }

  get(id: number, page: number, pageSize: number): Observable<PedimentLoad> {
    return this.http.get<PedimentLoad>(`${this.baseUrl}/cargas/${id}`, { params: { pagina: page, tamano: pageSize } });
  }

  errors(id: number, page: number, pageSize: number): Observable<PedimentErrorsResponse> {
    return this.http.get<PedimentErrorsResponse>(this.baseUrl + '/cargas/' + id + '/errores', { params: { pagina: page, tamano: pageSize } });
  }

  confirm(id: number): Observable<PedimentConfirmation> {
    return this.http.post<PedimentConfirmation>(`${this.baseUrl}/cargas/${id}/confirmacion`, {});
  }
}
