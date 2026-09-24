import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProfileActivity } from '../../domain/models/profile-permission.model';
import { ProfileRepository } from '../../domain/repositories/profile.repository';

/** Consulta el catálogo técnico read-only de actividades. */
@Injectable()
export class ListProfileActivitiesUseCase {
  private readonly repository = inject(ProfileRepository);
  execute(): Observable<ProfileActivity[]> { return this.repository.listActivities(); }
}
