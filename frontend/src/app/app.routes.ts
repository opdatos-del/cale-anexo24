import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';
import { permissionGuard } from '@core/guards/permission.guard';
import { ForbiddenComponent } from '@core/ui/forbidden/forbidden.component';
import { MainLayoutComponent } from '@layout/main-layout.component';

export const routes: Routes = [
  {
    path: 'login',
    loadChildren: () => import('@features/auth/auth.routes').then((routes) => routes.AUTH_ROUTES),
  },
  {
    path: 'forbidden',
    component: ForbiddenComponent,
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadChildren: () => import('@features/dashboard/dashboard.routes').then((routes) => routes.DASHBOARD_ROUTES),
      },
      {
        path: 'materiales',
        canActivate: [permissionGuard],
        data: { permission: 'MATERIALES_CONSULTAR' },
        loadChildren: () => import('@features/catalogs/materials/materials.routes').then((routes) => routes.MATERIALS_ROUTES),
      },
      {
        path: 'productos',
        canActivate: [permissionGuard],
        data: { permission: 'PRODUCTOS_CONSULTAR' },
        loadChildren: () => import('@features/catalogs/products/products.routes').then((routes) => routes.PRODUCTS_ROUTES),
      },
      {
        path: 'estructuras',
        canActivate: [permissionGuard],
        data: { permission: 'ESTRUCTURAS_CONSULTAR' },
        loadChildren: () => import('@features/catalogs/structures/structures.routes').then((routes) => routes.STRUCTURES_ROUTES),
      },
      {
        path: 'operaciones',
        canActivate: [permissionGuard],
        data: { permission: 'OPERACIONES_CONSULTAR' },
        loadChildren: () => import('@features/operations/operations.routes').then((routes) => routes.OPERATIONS_ROUTES),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
