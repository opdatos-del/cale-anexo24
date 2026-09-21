import { Routes } from '@angular/router';

export const OPERATIONS_ROUTES: Routes = [
  {
    path: 'entradas',
    loadChildren: () => import('./entries/entries.routes').then((routes) => routes.ENTRIES_ROUTES),
  },
  {
    path: 'salidas',
    loadChildren: () => import('./exits/exits.routes').then((routes) => routes.EXITS_ROUTES),
  },
];
