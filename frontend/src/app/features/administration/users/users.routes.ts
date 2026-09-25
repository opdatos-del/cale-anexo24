import { Routes } from '@angular/router';
import { ChangeUserExpirationUseCase } from './application/use-cases/change-user-expiration.use-case';
import { ChangeUserProfileUseCase } from './application/use-cases/change-user-profile.use-case';
import { ChangeUserStatusUseCase } from './application/use-cases/change-user-status.use-case';
import { CreateUserUseCase } from './application/use-cases/create-user.use-case';
import { GetUserUseCase } from './application/use-cases/get-user.use-case';
import { ResetUserPasswordUseCase } from './application/use-cases/reset-user-password.use-case';
import { SearchUsersUseCase } from './application/use-cases/search-users.use-case';
import { UpdateUserUseCase } from './application/use-cases/update-user.use-case';
import { LoadAllProfilesUseCase } from '@features/administration/profiles/application/use-cases/load-all-profiles.use-case';
import { ListProfilesUseCase } from '@features/administration/profiles/application/use-cases/list-profiles.use-case';
import { ProfileRepository } from '@features/administration/profiles/domain/repositories/profile.repository';
import { HttpProfileRepository } from '@features/administration/profiles/infrastructure/repositories/http-profile.repository';
import { UserRepository } from './domain/repositories/user.repository';
import { HttpUserRepository } from './infrastructure/repositories/http-user.repository';
import { UserListPage } from './presentation/pages/user-list/user-list.page';

export const USERS_ROUTES: Routes = [
  {
    path: '',
    component: UserListPage,
    providers: [
      { provide: UserRepository, useClass: HttpUserRepository },
      { provide: ProfileRepository, useClass: HttpProfileRepository },
      SearchUsersUseCase,
      GetUserUseCase,
      UpdateUserUseCase,
      CreateUserUseCase,
      ChangeUserStatusUseCase,
      ChangeUserProfileUseCase,
      ChangeUserExpirationUseCase,
      ResetUserPasswordUseCase,
      ListProfilesUseCase,
      LoadAllProfilesUseCase,
    ],
  },
];
