import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ProfilePage, ProfileSearchCriteria } from '../../domain/models/profile-administration.model';
import { ProfileRepository } from '../../domain/repositories/profile.repository';
import { ProfileApiService } from '../api/profile-api.service';
import { ProfileMapper } from '../api/mappers/profile.mapper';

/** Implementación HTTP del puerto read-only de perfiles. */
@Injectable()
export class HttpProfileRepository implements ProfileRepository {
  private readonly api = inject(ProfileApiService);

  search(criteria: ProfileSearchCriteria): Observable<ProfilePage> {
    return this.api.search(criteria).pipe(map(ProfileMapper.toPage));
  }
}
