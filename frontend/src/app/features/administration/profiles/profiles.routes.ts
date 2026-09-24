import { Routes } from '@angular/router';
import { CreateProfileUseCase } from './application/use-cases/create-profile.use-case';
import { ChangeProfileStatusUseCase } from './application/use-cases/change-profile-status.use-case';
import { GetProfilePermissionsUseCase } from './application/use-cases/get-profile-permissions.use-case';
import { ListProfileActivitiesUseCase } from './application/use-cases/list-profile-activities.use-case';
import { LoadAllProfilesUseCase } from './application/use-cases/load-all-profiles.use-case';
import { ReplaceProfilePermissionsUseCase } from './application/use-cases/replace-profile-permissions.use-case';
import { UpdateProfileNameUseCase } from './application/use-cases/update-profile-name.use-case';
import { ProfileRepository } from './domain/repositories/profile.repository';
import { HttpProfileRepository } from './infrastructure/repositories/http-profile.repository';
import { ProfileManagementPage } from './presentation/pages/profile-management/profile-management.page';

export const PROFILES_ROUTES: Routes = [
  {
    path: '',
    component: ProfileManagementPage,
    providers: [
      { provide: ProfileRepository, useClass: HttpProfileRepository },
      LoadAllProfilesUseCase,
      ListProfileActivitiesUseCase,
      GetProfilePermissionsUseCase,
      CreateProfileUseCase,
      UpdateProfileNameUseCase,
      ChangeProfileStatusUseCase,
      ReplaceProfilePermissionsUseCase,
    ],
  },
];
