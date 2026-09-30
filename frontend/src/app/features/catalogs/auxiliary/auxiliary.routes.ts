import { Routes } from '@angular/router';
import { AuxiliaryCatalogsPage } from './presentation/pages/auxiliary-catalogs/auxiliary-catalogs.page';
import { permissionGuard } from '@core/guards/permission.guard';

export const AUXILIARY_CATALOGS_ROUTES: Routes = [
  { path: '', canActivate: [permissionGuard], data: { permission: 'CATALOGOS_AUX_CONSULTAR' }, component: AuxiliaryCatalogsPage },
  {
    path: 'importaciones',
    canActivate: [permissionGuard],
    data: { permissions: ['MATERIALES_CARGAR', 'PRODUCTOS_CARGAR'] },
    loadChildren: () => import('../imports/catalog-import.routes').then((routes) => routes.CATALOG_IMPORT_ROUTES),
  },
];
