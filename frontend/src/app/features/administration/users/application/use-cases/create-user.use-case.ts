import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateUserCommand, UserAdministration } from '@features/administration/users/domain/models/user-administration.model';
import { UserRepository } from '@features/administration/users/domain/repositories/user.repository';

/** Crea un usuario administrativo con datos validados por la presentación y backend. */
@Injectable()
export class CreateUserUseCase {
  private readonly repository = inject(UserRepository);

  execute(command: CreateUserCommand): Observable<UserAdministration> {
    return this.repository.create(command);
  }
}
