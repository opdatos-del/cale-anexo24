import { expect, type APIRequestContext, type APIResponse, type Page, test as base } from '@playwright/test';
import { login } from './fixtures';

const ADMIN_API = process.env.E2E_BACKEND_URL || 'http://127.0.0.1:8080';
const PROFILE_KEY = 'E2E_AUTOMATION_PROFILE';
const USER_KEY = 'E2E_AUTOMATION_USER';
const PROFILE_ALT_KEY = 'E2E_AUTOMATION_PROFILE_ALT';
const USER_NAME = 'E2E Automation User';
const USER_EMAIL = 'e2e-automation@invalid.local';

export interface AdminFixtures {
  profile: { id: number; name: string; status: 'ACTIVO' | 'INACTIVO' };
  profileAlt: { id: number; name: string; status: 'ACTIVO' | 'INACTIVO' };
  user: { id: number; key: string; name: string; email: string; profileId: number; status: 'ACTIVO' | 'INACTIVO'; expiration: string | null };
  activityIds: number[];
  createdProfile: boolean;
  createdUser: boolean;
}

interface ApiProfile { id: number; nombre: string; estado: 'ACTIVO' | 'INACTIVO'; cantidadPermisos: number }
interface ApiUser { id: number; clave: string; nombre: string; correo: string; estado: 'ACTIVO' | 'INACTIVO'; vigencia: string | null; perfilId: number; perfilNombre: string }
interface Activity { id: number; clave: string; nombre: string }

export const adminTest = base.extend<{ adminFixtures: AdminFixtures }>({
  adminFixtures: [async ({ playwright }, use) => {
    const request = await playwright.request.newContext({ baseURL: ADMIN_API });
    const fixtures = await ensureFixtures(request);
    try {
      await use(fixtures);
    } finally {
      await restoreFixtures(request, fixtures);
      await request.dispose();
    }
  }, { scope: 'worker' }],
});

export async function adminLogin(page: Page): Promise<void> {
  await login(page);
  await expect(page).toHaveURL(/\/dashboard(?:$|[?/#])/);
}

export async function ensureFixtures(request: APIRequestContext): Promise<AdminFixtures> {
  const token = await obtainAdminToken(request);
  const auth = { Authorization: `Bearer ${token}` };
  const profilesResponse = await request.get('/api/v1/administracion/perfiles?pagina=1&tamano=100', { headers: auth });
  expect(profilesResponse.ok(), await safeFailure(profilesResponse)).toBeTruthy();
  const profilesBody = await profilesResponse.json() as { items: ApiProfile[] };
  const matchingProfiles = profilesBody.items.filter((profile) => profile.nombre === PROFILE_KEY);
  expect(matchingProfiles.length, `Se esperaba como máximo un perfil ${PROFILE_KEY}`).toBeLessThanOrEqual(1);

  let profile = matchingProfiles[0];
  let createdProfile = false;
  if (!profile) {
    const response = await request.post('/api/v1/administracion/perfiles', { headers: auth, data: { nombre: PROFILE_KEY } });
    expect(response.status(), await safeFailure(response)).toBe(201);
    profile = await response.json() as ApiProfile;
    createdProfile = true;
  }

  const activitiesResponse = await request.get('/api/v1/administracion/actividades', { headers: auth });
  expect(activitiesResponse.ok(), await safeFailure(activitiesResponse)).toBeTruthy();
  const activities = await activitiesResponse.json() as Activity[];
  const selectedActivities = activities.filter((activity) => ['MATERIALES_CONSULTAR', 'PRODUCTOS_CONSULTAR'].includes(activity.clave));
  expect(selectedActivities.length, 'Deben existir actividades sintéticas conocidas').toBe(2);
  const activityIds = selectedActivities.map((activity) => activity.id);
  await replacePermissions(request, auth, profile.id, activityIds);
  await ensureProfileActive(request, auth, profile.id);
  const profileAlt = await ensureNamedProfile(request, auth, PROFILE_ALT_KEY, []);

  const usersResponse = await request.get(`/api/v1/administracion/usuarios?clave=${encodeURIComponent(USER_KEY)}&pagina=1&tamano=100`, { headers: auth });
  expect(usersResponse.ok(), await safeFailure(usersResponse)).toBeTruthy();
  const usersBody = await usersResponse.json() as { items: ApiUser[] };
  const matchingUsers = usersBody.items.filter((user) => user.clave === USER_KEY);
  expect(matchingUsers.length, `Se esperaba como máximo un usuario ${USER_KEY}`).toBeLessThanOrEqual(1);

  let user = matchingUsers[0];
  let createdUser = false;
  const fixturePassword = requiredEnv('E2E_FIXTURE_PASSWORD');
  if (!user) {
    const response = await request.post('/api/v1/administracion/usuarios', {
      headers: auth,
      data: { clave: USER_KEY, nombre: USER_NAME, correo: USER_EMAIL, password: fixturePassword, vigencia: null, perfilId: profile.id },
    });
    expect(response.status(), await safeFailure(response)).toBe(201);
    user = await response.json() as ApiUser;
    createdUser = true;
  } else {
    await request.post(`/api/v1/administracion/usuarios/${user.id}/password`, { headers: auth, data: { password: fixturePassword } });
    await request.put(`/api/v1/administracion/usuarios/${user.id}`, { headers: auth, data: { nombre: USER_NAME, correo: USER_EMAIL } });
    await request.patch(`/api/v1/administracion/usuarios/${user.id}/perfil`, { headers: auth, data: { perfilId: profile.id } });
    await request.patch(`/api/v1/administracion/usuarios/${user.id}/vigencia`, { headers: auth, data: { vigencia: null } });
    await request.patch(`/api/v1/administracion/usuarios/${user.id}/estado`, { headers: auth, data: { estado: 'ACTIVO' } });
    user = await getUser(request, auth, user.id);
  }

  return {
    profile: { id: profile.id, name: profile.nombre, status: profile.estado },
    profileAlt,
    user: { id: user.id, key: user.clave, name: user.nombre, email: user.correo, profileId: user.perfilId, status: user.estado, expiration: user.vigencia },
    activityIds,
    createdProfile,
    createdUser,
  };
}

export async function restoreFixtures(request: APIRequestContext, fixtures: AdminFixtures): Promise<void> {
  const token = await obtainAdminToken(request);
  const headers = { Authorization: `Bearer ${token}` };
  const fixturePassword = requiredEnv('E2E_FIXTURE_PASSWORD');
  const operations: Promise<APIResponse>[] = [
    request.put(`/api/v1/administracion/perfiles/${fixtures.profile.id}`, { headers, data: { nombre: PROFILE_KEY } }),
    request.patch(`/api/v1/administracion/perfiles/${fixtures.profile.id}/estado`, { headers, data: { estado: 'ACTIVO' } }),
    replacePermissions(request, headers, fixtures.profile.id, fixtures.activityIds),
    request.put(`/api/v1/administracion/perfiles/${fixtures.profileAlt.id}`, { headers, data: { nombre: PROFILE_ALT_KEY } }),
    request.patch(`/api/v1/administracion/perfiles/${fixtures.profileAlt.id}/estado`, { headers, data: { estado: 'ACTIVO' } }),
    replacePermissions(request, headers, fixtures.profileAlt.id, []),
    request.put(`/api/v1/administracion/usuarios/${fixtures.user.id}`, { headers, data: { nombre: USER_NAME, correo: USER_EMAIL } }),
    request.patch(`/api/v1/administracion/usuarios/${fixtures.user.id}/perfil`, { headers, data: { perfilId: fixtures.profile.id } }),
    request.patch(`/api/v1/administracion/usuarios/${fixtures.user.id}/vigencia`, { headers, data: { vigencia: null } }),
    request.patch(`/api/v1/administracion/usuarios/${fixtures.user.id}/estado`, { headers, data: { estado: 'ACTIVO' } }),
    request.post(`/api/v1/administracion/usuarios/${fixtures.user.id}/password`, { headers, data: { password: fixturePassword } }),
  ];
  const results = await Promise.all(operations);
  const failed = results.find((response) => !response.ok());
  expect(failed, 'E2E_FIXTURE_CLEANUP_FAILED').toBeUndefined();
}

export async function apiContextWithAdmin(request: APIRequestContext): Promise<APIRequestContext> {
  const token = await obtainAdminToken(request);
  return request;
}

export async function loginWithCredentials(request: APIRequestContext, username: string, password: string): Promise<string> {
  const response = await request.post('/api/v1/auth/login', { data: { clave: username, password } });
  expect(response.status(), await safeFailure(response)).toBe(200);
  const body = await response.json() as { token: string };
  expect(body.token).toBeTruthy();
  return body.token;
}

async function obtainAdminToken(request: APIRequestContext): Promise<string> {
  return loginWithCredentials(request, requiredEnv('E2E_USERNAME'), requiredEnv('E2E_PASSWORD'));
}

async function getUser(request: APIRequestContext, headers: Record<string, string>, id: number): Promise<ApiUser> {
  const response = await request.get(`/api/v1/administracion/usuarios/${id}`, { headers });
  expect(response.ok(), await safeFailure(response)).toBeTruthy();
  return response.json() as Promise<ApiUser>;
}

async function replacePermissions(request: APIRequestContext, headers: Record<string, string>, profileId: number, activityIds: number[]): Promise<APIResponse> {
  const response = await request.put(`/api/v1/administracion/perfiles/${profileId}/permisos`, { headers, data: { actividadIds: activityIds } });
  expect(response.ok(), await safeFailure(response)).toBeTruthy();
  return response;
}

async function ensureProfileActive(request: APIRequestContext, headers: Record<string, string>, profileId: number): Promise<void> {
  const response = await request.patch(`/api/v1/administracion/perfiles/${profileId}/estado`, { headers, data: { estado: 'ACTIVO' } });
  expect(response.ok() || response.status() === 409, await safeFailure(response)).toBeTruthy();
}

async function ensureNamedProfile(request: APIRequestContext, headers: Record<string, string>, name: string, activityIds: number[]): Promise<{ id: number; name: string; status: 'ACTIVO' | 'INACTIVO' }> {
  const response = await request.get('/api/v1/administracion/perfiles?pagina=1&tamano=100', { headers });
  expect(response.ok(), await safeFailure(response)).toBeTruthy();
  const body = await response.json() as { items: ApiProfile[] };
  const matches = body.items.filter((profile) => profile.nombre === name);
  expect(matches.length, `Se esperaba como máximo un perfil ${name}`).toBeLessThanOrEqual(1);
  let profile = matches[0];
  if (!profile) {
    const created = await request.post('/api/v1/administracion/perfiles', { headers, data: { nombre: name } });
    expect(created.status(), await safeFailure(created)).toBe(201);
    profile = await created.json() as ApiProfile;
  }
  await replacePermissions(request, headers, profile.id, activityIds);
  await ensureProfileActive(request, headers, profile.id);
  return { id: profile.id, name: profile.nombre, status: 'ACTIVO' };
}

async function safeFailure(response: APIResponse): Promise<string> {
  return `${new URL(response.url()).pathname} returned ${response.status()}`;
}

function requiredEnv(name: string): string {
  const value = process.env[name];
  if (!value) throw new Error(`${name} es obligatorio y debe existir sólo en el entorno del proceso.`);
  return value;
}

export { PROFILE_KEY, USER_EMAIL, USER_KEY, USER_NAME };
