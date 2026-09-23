import { Routes } from '@angular/router';
import { ChangeUserExpirationUseCase } from './application/use-cases/change-user-expiration.use-case';
import { ChangeUserStatusUseCase } from './application/use-cases/change-user-status.use-case';
import { GetUserUseCase } from './application/use-cases/get-user.use-case';
import { ResetUserPasswordUseCase } from './application/use-cases/reset-user-password.use-case';
import { SearchUsersUseCase } from './application/use-cases/search-users.use-case';
import { UpdateUserUseCase } from './application/use-cases/update-user.use-case';
import { UserRepository } from './domain/repositories/user.repository';
import { HttpUserRepository } from './infrastructure/repositories/http-user.repository';
import { UserListPage } from './presentation/pages/user-list/user-list.page';

export const USERS_ROUTES: Routes = [
  {
    path: '',
    component: UserListPage,
    providers: [
      { provide: UserRepository, useClass: HttpUserRepository },
      SearchUsersUseCase,
      GetUserUseCase,
      UpdateUserUseCase,
      ChangeUserStatusUseCase,
      ChangeUserExpirationUseCase,
      ResetUserPasswordUseCase,
    ],
  },
];
