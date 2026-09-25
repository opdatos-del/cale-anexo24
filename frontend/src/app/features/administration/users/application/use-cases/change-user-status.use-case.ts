import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ChangeUserStatusCommand, UserAdministration } from '@features/administration/users/domain/models/user-administration.model';
import { UserRepository } from '@features/administration/users/domain/repositories/user.repository';

/** Activa o inactiva un usuario. */
@Injectable()
export class ChangeUserStatusUseCase {
  private readonly repository = inject(UserRepository);
  execute(id: number, command: ChangeUserStatusCommand): Observable<UserAdministration> {
    return this.repository.changeStatus(id, command);
  }
}
