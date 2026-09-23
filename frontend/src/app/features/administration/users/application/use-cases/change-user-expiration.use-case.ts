import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ChangeUserExpirationCommand, UserAdministration } from '../../domain/models/user-administration.model';
import { UserRepository } from '../../domain/repositories/user.repository';

/** Cambia la vigencia de un usuario. */
@Injectable()
export class ChangeUserExpirationUseCase {
  private readonly repository = inject(UserRepository);
  execute(id: number, command: ChangeUserExpirationCommand): Observable<UserAdministration> {
    return this.repository.changeExpiration(id, command);
  }
}
