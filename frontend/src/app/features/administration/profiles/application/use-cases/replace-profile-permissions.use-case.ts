import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProfilePermissions } from '../../domain/models/profile-permission.model';
import { ProfileRepository } from '../../domain/repositories/profile.repository';

/** Envía el conjunto final completo de actividades asignadas. */
@Injectable()
export class ReplaceProfilePermissionsUseCase {
  private readonly repository = inject(ProfileRepository);
  execute(profileId: number, activityIds: number[]): Observable<ProfilePermissions> {
    return this.repository.replacePermissions(profileId, activityIds);
  }
}
