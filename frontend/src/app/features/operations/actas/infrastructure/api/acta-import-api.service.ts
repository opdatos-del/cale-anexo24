import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActaConfirmation, ActaError, ActaLoad } from '@features/operations/actas/domain/models/acta-import.model';

/** Contratos HTTP de la operación de carga de actas (nunca bajo /catalogos). */
@Injectable({ providedIn: 'root' })
export class ActaImportApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/operaciones/actas/importaciones';
  private readonly operacionUrl = '/api/v1/operaciones/actas';

  upload(file: File): Observable<ActaLoad> {
    const form = new FormData();
    form.append('archivo', file, file.name);
    return this.http.post<ActaLoad>(this.baseUrl, form);
  }

  get(id: number, page: number, pageSize: number): Observable<ActaLoad> {
    return this.http.get<ActaLoad>(`${this.baseUrl}/${id}`, { params: { pagina: page, tamano: pageSize } });
  }

  errors(id: number, page: number, pageSize: number): Observable<ActaError[]> {
    return this.http.get<ActaError[]>(`${this.baseUrl}/${id}/errores`, { params: { pagina: page, tamano: pageSize } });
  }

  confirm(id: number): Observable<ActaConfirmation> {
    return this.http.post<ActaConfirmation>(`${this.operacionUrl}/${id}/confirmacion`, {});
  }
}
