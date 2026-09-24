import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProfileAdministration } from '../../domain/models/profile-administration.model';
import { ProfileRepository } from '../../domain/repositories/profile.repository';

/** Crea perfil con nombre; backend asigna ACTIVO y cero permisos. */
@Injectable()
export class CreateProfileUseCase {
  private readonly repository = inject(ProfileRepository);
  execute(name: string): Observable<ProfileAdministration> { return this.repository.create(name); }
}
