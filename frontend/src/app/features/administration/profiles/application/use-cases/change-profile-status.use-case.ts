import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProfileAdministration, ProfileStatus } from '@features/administration/profiles/domain/models/profile-administration.model';
import { ProfileRepository } from '@features/administration/profiles/domain/repositories/profile.repository';

/** Cambia estado del perfil; backend decide guardrail. */
@Injectable()
export class ChangeProfileStatusUseCase {
  private readonly repository = inject(ProfileRepository);
  execute(profileId: number, status: ProfileStatus): Observable<ProfileAdministration> {
    return this.repository.changeStatus(profileId, status);
  }
}
