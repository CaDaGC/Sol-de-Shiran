import { Routes } from '@angular/router';
import { BusinessLayoutComponent } from './layouts/business-layout/business-layout.component';
import { CatalogoComponent } from './pages/catalogo/catalogo.component';
import { LoginComponent } from './pages/login/login.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: '',
    component: BusinessLayoutComponent,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'catalogo'
      },
      {
        path: 'catalogo',
        component: CatalogoComponent
      }
      // Agrega aquí las futuras rutas de negocio como nuevas HU.
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
