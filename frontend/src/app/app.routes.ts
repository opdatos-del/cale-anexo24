import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { LoginComponent } from './auth/login.component';
import { MaterialesComponent } from './catalogos/materiales.component';

export const routes: Routes = [
  { path: '', redirectTo: '/materiales', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'materiales', component: MaterialesComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '/materiales' },
];