import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProfilePage, ProfileSearchCriteria } from '../../domain/models/profile-administration.model';
import { ProfileRepository } from '../../domain/repositories/profile.repository';

/** Caso de uso de consulta paginada read-only de perfiles. */
@Injectable()
export class ListProfilesUseCase {
  private readonly repository = inject(ProfileRepository);

  execute(criteria: ProfileSearchCriteria): Observable<ProfilePage> {
    return this.repository.search(criteria);
  }
}
