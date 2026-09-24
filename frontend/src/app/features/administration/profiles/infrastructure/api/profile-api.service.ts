import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProfileSearchCriteria, ProfileStatus } from '../../domain/models/profile-administration.model';
import { ProfilePageResponseDto, ProfileResponseDto } from './dto/profile.dto';
import {
  ProfileActivityResponseDto,
  ProfileNameRequestDto,
  ProfilePermissionsResponseDto,
  ProfileStatusRequestDto,
  ReplaceProfilePermissionsRequestDto,
} from './dto/profile-management.dto';

/** Cliente del contrato HTTP V1 de administración de perfiles. */
@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/administracion/perfiles';
  private readonly activitiesUrl = '/api/v1/administracion/actividades';

  search(criteria: ProfileSearchCriteria): Observable<ProfilePageResponseDto> {
    let params = new HttpParams().set('pagina', criteria.page).set('tamano', criteria.pageSize);
    const name = criteria.name.trim();
    if (name) params = params.set('nombre', name);
    if (criteria.status) params = params.set('estado', criteria.status);
    return this.http.get<ProfilePageResponseDto>(this.baseUrl, { params });
  }

  listActivities(): Observable<ProfileActivityResponseDto[]> {
    return this.http.get<ProfileActivityResponseDto[]>(this.activitiesUrl);
  }

  getPermissions(profileId: number): Observable<ProfilePermissionsResponseDto> {
    return this.http.get<ProfilePermissionsResponseDto>(`${this.baseUrl}/${profileId}/permisos`);
  }

  create(name: string): Observable<ProfileResponseDto> {
    const body: ProfileNameRequestDto = { nombre: name };
    return this.http.post<ProfileResponseDto>(this.baseUrl, body);
  }

  updateName(profileId: number, name: string): Observable<ProfileResponseDto> {
    const body: ProfileNameRequestDto = { nombre: name };
    return this.http.put<ProfileResponseDto>(`${this.baseUrl}/${profileId}`, body);
  }

  changeStatus(profileId: number, status: ProfileStatus): Observable<ProfileResponseDto> {
    const body: ProfileStatusRequestDto = { estado: status };
    return this.http.patch<ProfileResponseDto>(`${this.baseUrl}/${profileId}/estado`, body);
  }

  replacePermissions(profileId: number, activityIds: number[]): Observable<ProfilePermissionsResponseDto> {
    const body: ReplaceProfilePermissionsRequestDto = { actividadIds: activityIds };
    return this.http.put<ProfilePermissionsResponseDto>(`${this.baseUrl}/${profileId}/permisos`, body);
  }
}
