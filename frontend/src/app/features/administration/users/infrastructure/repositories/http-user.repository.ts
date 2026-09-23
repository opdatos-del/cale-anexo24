import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import {
  ChangeUserExpirationCommand,
  ChangeUserStatusCommand,
  ResetUserPasswordCommand,
  UpdateUserCommand,
  UserAdministration,
  UserPage,
  UserSearchCriteria,
} from '../../domain/models/user-administration.model';
import { UserRepository } from '../../domain/repositories/user.repository';
import { UserApiService } from '../api/user-api.service';
import { UserMapper } from '../api/mappers/user.mapper';

/** Adaptador HTTP del puerto de usuarios. */
@Injectable()
export class HttpUserRepository implements UserRepository {
  private readonly api = inject(UserApiService);

  search(criteria: UserSearchCriteria): Observable<UserPage> {
    return this.api.search(criteria).pipe(map(UserMapper.toPage));
  }

  getById(id: number): Observable<UserAdministration> {
    return this.api.getById(id).pipe(map(UserMapper.toDomain));
  }

  update(id: number, command: UpdateUserCommand): Observable<UserAdministration> {
    return this.api.update(id, UserMapper.toUpdateRequest(command)).pipe(map(UserMapper.toDomain));
  }

  changeStatus(id: number, command: ChangeUserStatusCommand): Observable<UserAdministration> {
    return this.api.changeStatus(id, UserMapper.toStatusRequest(command)).pipe(map(UserMapper.toDomain));
  }

  changeExpiration(id: number, command: ChangeUserExpirationCommand): Observable<UserAdministration> {
    return this.api.changeExpiration(id, UserMapper.toExpirationRequest(command)).pipe(map(UserMapper.toDomain));
  }

  resetPassword(id: number, command: ResetUserPasswordCommand): Observable<void> {
    return this.api.resetPassword(id, UserMapper.toPasswordRequest(command));
  }
}
