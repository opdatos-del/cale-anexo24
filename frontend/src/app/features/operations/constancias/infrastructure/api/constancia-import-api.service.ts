import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ConstanciaConfirmation, ConstanciaError, ConstanciaLoad } from '@features/operations/constancias/domain/models/constancia-import.model';

/** Contratos HTTP de la operación de carga de constancias (nunca bajo /catalogos). */
@Injectable({ providedIn: 'root' })
export class ConstanciaImportApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/operaciones/constancias/importaciones';
  private readonly operacionUrl = '/api/v1/operaciones/constancias';

  upload(file: File): Observable<ConstanciaLoad> {
    const form = new FormData();
    form.append('archivo', file, file.name);
    return this.http.post<ConstanciaLoad>(this.baseUrl, form);
  }

  get(id: number, page: number, pageSize: number): Observable<ConstanciaLoad> {
    return this.http.get<ConstanciaLoad>(`${this.baseUrl}/${id}`, { params: { pagina: page, tamano: pageSize } });
  }

  errors(id: number, page: number, pageSize: number): Observable<ConstanciaError[]> {
    return this.http.get<ConstanciaError[]>(`${this.baseUrl}/${id}/errores`, { params: { pagina: page, tamano: pageSize } });
  }

  confirm(id: number): Observable<ConstanciaConfirmation> {
    return this.http.post<ConstanciaConfirmation>(`${this.operacionUrl}/${id}/confirmacion`, {});
  }
}
