import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProfileSearchCriteria } from '../../domain/models/profile-administration.model';
import { ProfilePageResponseDto } from './dto/profile.dto';

/** Cliente del contrato HTTP V1 de administración de perfiles. */
@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/administracion/perfiles';

  search(criteria: ProfileSearchCriteria): Observable<ProfilePageResponseDto> {
    let params = new HttpParams().set('pagina', criteria.page).set('tamano', criteria.pageSize);
    const name = criteria.name.trim();
    if (name) params = params.set('nombre', name);
    if (criteria.status) params = params.set('estado', criteria.status);
    return this.http.get<ProfilePageResponseDto>(this.baseUrl, { params });
  }
}
