import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { UpdateUserCommand, UserAdministration } from '@features/administration/users/domain/models/user-administration.model';
import { UserRepository } from '@features/administration/users/domain/repositories/user.repository';

/** Actualiza nombre y correo de un usuario. */
@Injectable()
export class UpdateUserUseCase {
  private readonly repository = inject(UserRepository);
  execute(id: number, command: UpdateUserCommand): Observable<UserAdministration> {
    return this.repository.update(id, command);
  }
}
