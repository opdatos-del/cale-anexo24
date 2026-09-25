import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProfilePermissions } from '@features/administration/profiles/domain/models/profile-permission.model';
import { ProfileRepository } from '@features/administration/profiles/domain/repositories/profile.repository';

/** Consulta actividades asignadas a un perfil. */
@Injectable()
export class GetProfilePermissionsUseCase {
  private readonly repository = inject(ProfileRepository);
  execute(profileId: number): Observable<ProfilePermissions> { return this.repository.getPermissions(profileId); }
}
