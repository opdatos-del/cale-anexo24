import { Observable } from 'rxjs';
import {
  ChangeUserExpirationCommand,
  ChangeUserStatusCommand,
  ResetUserPasswordCommand,
  UpdateUserCommand,
  UserAdministration,
  UserPage,
  UserSearchCriteria,
} from '../models/user-administration.model';

/** Puerto de administración de usuarios. */
export abstract class UserRepository {
  abstract search(criteria: UserSearchCriteria): Observable<UserPage>;
  abstract getById(id: number): Observable<UserAdministration>;
  abstract update(id: number, command: UpdateUserCommand): Observable<UserAdministration>;
  abstract changeStatus(id: number, command: ChangeUserStatusCommand): Observable<UserAdministration>;
  abstract changeExpiration(id: number, command: ChangeUserExpirationCommand): Observable<UserAdministration>;
  abstract resetPassword(id: number, command: ResetUserPasswordCommand): Observable<void>;
}
