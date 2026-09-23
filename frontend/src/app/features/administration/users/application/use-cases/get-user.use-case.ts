import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { UserAdministration } from '../../domain/models/user-administration.model';
import { UserRepository } from '../../domain/repositories/user.repository';

/** Obtiene el detalle actualizado de un usuario. */
@Injectable()
export class GetUserUseCase {
  private readonly repository = inject(UserRepository);
  execute(id: number): Observable<UserAdministration> {
    return this.repository.getById(id);
  }
}
