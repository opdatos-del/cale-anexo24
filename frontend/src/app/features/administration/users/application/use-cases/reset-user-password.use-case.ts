import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ResetUserPasswordCommand } from '@features/administration/users/domain/models/user-administration.model';
import { UserRepository } from '@features/administration/users/domain/repositories/user.repository';

/** Restablece la contraseña sin exponerla en respuestas. */
@Injectable()
export class ResetUserPasswordUseCase {
  private readonly repository = inject(UserRepository);
  execute(id: number, command: ResetUserPasswordCommand): Observable<void> {
    return this.repository.resetPassword(id, command);
  }
}
