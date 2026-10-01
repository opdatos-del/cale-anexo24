import { Routes } from '@angular/router';
import { AuxiliaryCatalogsPage } from './presentation/pages/auxiliary-catalogs/auxiliary-catalogs.page';
import { permissionGuard } from '@core/guards/permission.guard';

export const AUXILIARY_CATALOGS_ROUTES: Routes = [
  { path: '', canActivate: [permissionGuard], data: { permission: 'CATALOGOS_AUX_CONSULTAR' }, component: AuxiliaryCatalogsPage },
  {
    path: 'datos-generales',
    canActivate: [permissionGuard],
    data: { permission: 'CATALOGOS_AUX_CONSULTAR' },
    loadChildren: () => import('@features/catalogs/general-data/general-data.routes').then((routes) => routes.GENERAL_DATA_ROUTES),
  },
  {
    path: 'socios-comerciales',
    canActivate: [permissionGuard],
    data: { permission: 'CATALOGOS_AUX_CONSULTAR' },
    loadChildren: () => import('../business-parties/business-parties.routes').then((routes) => routes.BUSINESS_PARTIES_ROUTES),
  },
  {
    path: 'importaciones',
    canActivate: [permissionGuard],
    data: { permissions: ['MATERIALES_CARGAR', 'PRODUCTOS_CARGAR'] },
    loadChildren: () => import('../imports/catalog-import.routes').then((routes) => routes.CATALOG_IMPORT_ROUTES),
  },
];
