import { Routes } from '@angular/router';
import { BusinessLayoutComponent } from './layouts/business-layout/business-layout.component';
import { DetalleCabanaComponent } from './pages/detalle-cabana/detalle-cabana.component';
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
      },
      {
        path: 'catalogo/:id',
        component: DetalleCabanaComponent
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
