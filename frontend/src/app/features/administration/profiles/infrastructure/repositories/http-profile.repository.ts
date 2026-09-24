import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ProfileAdministration, ProfilePage, ProfileSearchCriteria, ProfileStatus } from '../../domain/models/profile-administration.model';
import { ProfileActivity, ProfilePermissions } from '../../domain/models/profile-permission.model';
import { ProfileRepository } from '../../domain/repositories/profile.repository';
import { ProfileApiService } from '../api/profile-api.service';
import { ProfileMapper } from '../api/mappers/profile.mapper';
import { ProfileManagementMapper } from '../api/mappers/profile-management.mapper';

/** Implementación HTTP del puerto read-only de perfiles. */
@Injectable()
export class HttpProfileRepository implements ProfileRepository {
  private readonly api = inject(ProfileApiService);

  search(criteria: ProfileSearchCriteria): Observable<ProfilePage> {
    return this.api.search(criteria).pipe(map(ProfileMapper.toPage));
  }

  listActivities(): Observable<ProfileActivity[]> {
    return this.api.listActivities().pipe(map((items) => items.map(ProfileManagementMapper.toActivity)));
  }

  getPermissions(profileId: number): Observable<ProfilePermissions> {
    return this.api.getPermissions(profileId).pipe(map(ProfileManagementMapper.toPermissions));
  }

  create(name: string): Observable<ProfileAdministration> {
    return this.api.create(name).pipe(map(ProfileManagementMapper.toProfile));
  }

  updateName(profileId: number, name: string): Observable<ProfileAdministration> {
    return this.api.updateName(profileId, name).pipe(map(ProfileManagementMapper.toProfile));
  }

  changeStatus(profileId: number, status: ProfileStatus): Observable<ProfileAdministration> {
    return this.api.changeStatus(profileId, status).pipe(map(ProfileManagementMapper.toProfile));
  }

  replacePermissions(profileId: number, activityIds: number[]): Observable<ProfilePermissions> {
    return this.api.replacePermissions(profileId, activityIds).pipe(map(ProfileManagementMapper.toPermissions));
  }
}
