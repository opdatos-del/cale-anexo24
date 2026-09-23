import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ChangeUserStatusCommand, UserAdministration } from '../../domain/models/user-administration.model';
import { UserRepository } from '../../domain/repositories/user.repository';

/** Activa o inactiva un usuario. */
@Injectable()
export class ChangeUserStatusUseCase {
  private readonly repository = inject(UserRepository);
  execute(id: number, command: ChangeUserStatusCommand): Observable<UserAdministration> {
    return this.repository.changeStatus(id, command);
  }
}
