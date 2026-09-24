import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProfileAdministration } from '../../domain/models/profile-administration.model';
import { ProfileRepository } from '../../domain/repositories/profile.repository';

/** Actualiza exclusivamente el nombre del perfil. */
@Injectable()
export class UpdateProfileNameUseCase {
  private readonly repository = inject(ProfileRepository);
  execute(profileId: number, name: string): Observable<ProfileAdministration> {
    return this.repository.updateName(profileId, name);
  }
}
