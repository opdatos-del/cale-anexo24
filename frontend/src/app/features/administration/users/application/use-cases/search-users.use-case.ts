import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { UserPage, UserSearchCriteria } from '../../domain/models/user-administration.model';
import { UserRepository } from '../../domain/repositories/user.repository';

/** Consulta usuarios con filtros y paginación. */
@Injectable()
export class SearchUsersUseCase {
  private readonly repository = inject(UserRepository);
  execute(criteria: UserSearchCriteria): Observable<UserPage> {
    return this.repository.search(criteria);
  }
}
