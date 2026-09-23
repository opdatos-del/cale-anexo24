import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { UserSearchCriteria } from '../../domain/models/user-administration.model';
import {
  ChangeUserExpirationRequestDto,
  ChangeUserStatusRequestDto,
  ResetUserPasswordRequestDto,
  UpdateUserRequestDto,
  UserPageResponseDto,
  UserResponseDto,
} from './dto/user.dto';

/** Cliente del contrato HTTP V1 de administración de usuarios. */
@Injectable({ providedIn: 'root' })
export class UserApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/administracion/usuarios';

  search(criteria: UserSearchCriteria): Observable<UserPageResponseDto> {
    let params = new HttpParams().set('pagina', criteria.page).set('tamano', criteria.pageSize);
    const filters: readonly [string, string][] = [
      ['clave', criteria.key.trim()],
      ['nombre', criteria.name.trim()],
      ['correo', criteria.email.trim()],
    ];
    for (const [name, value] of filters) {
      if (value) params = params.set(name, value);
    }
    if (criteria.status) params = params.set('estado', criteria.status);
    return this.http.get<UserPageResponseDto>(this.baseUrl, { params });
  }

  getById(id: number): Observable<UserResponseDto> {
    return this.http.get<UserResponseDto>(`${this.baseUrl}/${id}`);
  }

  update(id: number, request: UpdateUserRequestDto): Observable<UserResponseDto> {
    return this.http.put<UserResponseDto>(`${this.baseUrl}/${id}`, request);
  }

  changeStatus(id: number, request: ChangeUserStatusRequestDto): Observable<UserResponseDto> {
    return this.http.patch<UserResponseDto>(`${this.baseUrl}/${id}/estado`, request);
  }

  changeExpiration(id: number, request: ChangeUserExpirationRequestDto): Observable<UserResponseDto> {
    return this.http.patch<UserResponseDto>(`${this.baseUrl}/${id}/vigencia`, request);
  }

  resetPassword(id: number, request: ResetUserPasswordRequestDto): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/password`, request);
  }
}
