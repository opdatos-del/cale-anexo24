import { Observable } from 'rxjs';
import { ProfileActivity, ProfilePermissions } from '@features/administration/profiles/domain/models/profile-permission.model';
import { ProfileAdministration, ProfilePage, ProfileSearchCriteria, ProfileStatus } from '@features/administration/profiles/domain/models/profile-administration.model';

/** Puerto de consulta read-only de perfiles administrativos. */
export abstract class ProfileRepository {
  abstract search(criteria: ProfileSearchCriteria): Observable<ProfilePage>;
  abstract listActivities(): Observable<ProfileActivity[]>;
  abstract getPermissions(profileId: number): Observable<ProfilePermissions>;
  abstract create(name: string): Observable<ProfileAdministration>;
  abstract updateName(profileId: number, name: string): Observable<ProfileAdministration>;
  abstract changeStatus(profileId: number, status: ProfileStatus): Observable<ProfileAdministration>;
  abstract replacePermissions(profileId: number, activityIds: number[]): Observable<ProfilePermissions>;
}
