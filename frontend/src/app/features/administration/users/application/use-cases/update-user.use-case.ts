import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { UpdateUserCommand, UserAdministration } from '../../domain/models/user-administration.model';
import { UserRepository } from '../../domain/repositories/user.repository';

/** Actualiza nombre y correo de un usuario. */
@Injectable()
export class UpdateUserUseCase {
  private readonly repository = inject(UserRepository);
  execute(id: number, command: UpdateUserCommand): Observable<UserAdministration> {
    return this.repository.update(id, command);
  }
}
