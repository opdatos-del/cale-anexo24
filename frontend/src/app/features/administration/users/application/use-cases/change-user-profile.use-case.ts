import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ChangeUserProfileCommand, UserAdministration } from '../../domain/models/user-administration.model';
import { UserRepository } from '../../domain/repositories/user.repository';

/** Cambia el perfil asignado a un usuario. */
@Injectable()
export class ChangeUserProfileUseCase {
  private readonly repository = inject(UserRepository);

  execute(id: number, command: ChangeUserProfileCommand): Observable<UserAdministration> {
    return this.repository.changeProfile(id, command);
  }
}
