import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SavedQuery, SavedQueryScope, SaveQueryRequest } from '@features/saved-queries/domain/saved-query.model';

/** Cliente tipado de presets; sólo intercambia metadatos y filtros estructurados. */
@Injectable({ providedIn: 'root' })
export class SavedQueriesApi {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/v1/consultas-guardadas';

  list(scope?: SavedQueryScope): Observable<SavedQuery[]> {
    const params = scope ? new HttpParams().set('alcance', scope) : undefined;
    return this.http.get<SavedQuery[]>(this.url, { params });
  }
  create(request: SaveQueryRequest): Observable<SavedQuery> { return this.http.post<SavedQuery>(this.url, request); }
  update(id: number, request: SaveQueryRequest): Observable<SavedQuery> { return this.http.put<SavedQuery>(`${this.url}/${id}`, request); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}
