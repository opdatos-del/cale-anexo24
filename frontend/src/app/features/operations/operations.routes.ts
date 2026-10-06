import { Routes } from '@angular/router';
import { permissionGuard } from '@core/guards/permission.guard';

export const OPERATIONS_ROUTES: Routes = [
  {
    path: 'entradas',
    loadChildren: () => import('./entries/entries.routes').then((routes) => routes.ENTRIES_ROUTES),
  },
  {
    path: 'salidas',
    loadChildren: () => import('./exits/exits.routes').then((routes) => routes.EXITS_ROUTES),
  },
  {
    path: 'materiales-utilizados',
    loadChildren: () => import('./usedmaterials/used-materials.routes').then((routes) => routes.USED_MATERIALS_ROUTES),
  },
  {
    path: 'activos-fijos',
    loadChildren: () => import('./fixed-assets/fixed-assets.routes').then((routes) => routes.FIXED_ASSETS_ROUTES),
  },
  {
    path: 'pedimentos',
    canActivate: [permissionGuard],
    data: { permission: 'PEDIMENTOS_CARGAR' },
    loadChildren: () => import('./pediments/pediments.routes').then((routes) => routes.PEDIMENTS_ROUTES),
  },
  {
    path: 'actas',
    canActivate: [permissionGuard],
    data: { permission: 'ACTAS_CARGAR' },
    loadChildren: () => import('./actas/actas.routes').then((routes) => routes.ACTAS_ROUTES),
  },
  {
    path: 'constancias',
    canActivate: [permissionGuard],
    data: { permission: 'CONSTANCIAS_CARGAR' },
    loadChildren: () => import('./constancias/constancias.routes').then((routes) => routes.CONSTANCIAS_ROUTES),
  },
];
