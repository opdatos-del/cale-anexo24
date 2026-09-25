import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { UserPage, UserSearchCriteria } from '@features/administration/users/domain/models/user-administration.model';
import { UserRepository } from '@features/administration/users/domain/repositories/user.repository';

/** Consulta usuarios con filtros y paginación. */
@Injectable()
export class SearchUsersUseCase {
  private readonly repository = inject(UserRepository);
  execute(criteria: UserSearchCriteria): Observable<UserPage> {
    return this.repository.search(criteria);
  }
}
