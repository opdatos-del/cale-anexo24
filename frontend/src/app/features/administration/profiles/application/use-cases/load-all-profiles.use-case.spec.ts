import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ProfilePage } from '@features/administration/profiles/domain/models/profile-administration.model';
import { ProfileRepository } from '@features/administration/profiles/domain/repositories/profile.repository';
import { LoadAllProfilesUseCase } from './load-all-profiles.use-case';

const page = (items: ProfilePage['items'], total: number, number: number): ProfilePage => ({
  items,
  total,
  page: number,
  pageSize: 100,
});

describe('LoadAllProfilesUseCase', () => {
  const search = vi.fn();
  let useCase: LoadAllProfilesUseCase;

  beforeEach(() => {
    search.mockReset();
    TestBed.configureTestingModule({
      providers: [
        LoadAllProfilesUseCase,
        { provide: ProfileRepository, useValue: { search } },
      ],
    });
    useCase = TestBed.inject(LoadAllProfilesUseCase);
  });

  it('resuelve totales de hasta 100 en una sola solicitud', async () => {
    search.mockReturnValue(of(page([{ id: 1, name: 'Administrador', status: 'ACTIVO', permissionCount: 10 }], 1, 1)));

    await expect(firstValueFrom(useCase.execute())).resolves.toEqual([
      { id: 1, name: 'Administrador', status: 'ACTIVO', permissionCount: 10 },
    ]);
    expect(search).toHaveBeenCalledTimes(1);
    expect(search).toHaveBeenCalledWith({ name: '', status: null, page: 1, pageSize: 100 });
  });

  it('carga páginas mayores a 100 en orden y conserva el filtro de estado', async () => {
    search.mockImplementation((criteria) => {
      if (criteria.page === 1) return of(page([{ id: 1, name: 'Administrador', status: 'ACTIVO', permissionCount: 10 }], 201, 1));
      if (criteria.page === 2) return of(page([{ id: 2, name: 'Auditor', status: 'ACTIVO', permissionCount: 5 }], 201, 2));
      return of(page([{ id: 3, name: 'Operador', status: 'ACTIVO', permissionCount: 4 }], 201, 3));
    });

    await expect(firstValueFrom(useCase.execute('ACTIVO'))).resolves.toEqual([
      { id: 1, name: 'Administrador', status: 'ACTIVO', permissionCount: 10 },
      { id: 2, name: 'Auditor', status: 'ACTIVO', permissionCount: 5 },
      { id: 3, name: 'Operador', status: 'ACTIVO', permissionCount: 4 },
    ]);
    expect(search).toHaveBeenCalledTimes(3);
    expect(search.mock.calls.map(([criteria]) => criteria)).toEqual([
      { name: '', status: 'ACTIVO', page: 1, pageSize: 100 },
      { name: '', status: 'ACTIVO', page: 2, pageSize: 100 },
      { name: '', status: 'ACTIVO', page: 3, pageSize: 100 },
    ]);
  });

  it('detiene la carga si una página posterior está vacía', async () => {
    search.mockImplementation((criteria) => of(criteria.page === 1
      ? page([{ id: 1, name: 'Administrador', status: 'ACTIVO', permissionCount: 10 }], 500, 1)
      : page([], 500, criteria.page)));

    await expect(firstValueFrom(useCase.execute())).resolves.toEqual([
      { id: 1, name: 'Administrador', status: 'ACTIVO', permissionCount: 10 },
    ]);
    expect(search).toHaveBeenCalledTimes(2);
  });
});
