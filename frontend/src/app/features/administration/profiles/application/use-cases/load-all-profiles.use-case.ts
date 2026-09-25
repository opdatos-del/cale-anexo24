import { Injectable, inject } from '@angular/core';
import { EMPTY, Observable, defer, expand, map, reduce } from 'rxjs';
import { ProfileAdministration, ProfilePage, ProfileStatus } from '@features/administration/profiles/domain/models/profile-administration.model';
import { ProfileRepository } from '@features/administration/profiles/domain/repositories/profile.repository';

const MAX_PAGE_SIZE = 100;

/** Carga todos los perfiles preservando el orden paginado del backend. */
@Injectable()
export class LoadAllProfilesUseCase {
  private readonly repository = inject(ProfileRepository);

  execute(status: ProfileStatus | null = null): Observable<ProfileAdministration[]> {
    return defer(() => {
      let requestedPage = 1;

      const search = (page: number): Observable<ProfilePage> => this.repository.search({
        name: '',
        status,
        page,
        pageSize: MAX_PAGE_SIZE,
      });

      return search(requestedPage).pipe(
        expand((result) => {
          const totalPages = Math.ceil(result.total / MAX_PAGE_SIZE);
          if (result.items.length === 0 || requestedPage >= totalPages) return EMPTY;
          requestedPage += 1;
          return search(requestedPage);
        }),
        map((result) => result.items),
        reduce((profiles, items) => profiles.concat(items), [] as ProfileAdministration[]),
      );
    });
  }
}
