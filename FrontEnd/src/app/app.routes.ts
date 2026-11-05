import { PagoResultadoComponent } from './shared/pago-modal/pago-resultado.component';
import { Routes } from '@angular/router';
import { BusinessLayoutComponent } from './layouts/business-layout/business-layout.component';
import { DetalleCabanaComponent } from './pages/detalle-cabana/detalle-cabana.component';
import { CatalogoComponent } from './pages/catalogo/catalogo.component';
import { LoginComponent } from './pages/login/login.component';
import { guardianGuard } from './guards/guardian.guard';
import { clienteGuard } from './guards/cliente.guard';

export const routes: Routes = [
  { path: 'pago/resultado', component: PagoResultadoComponent },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: '',
    component: BusinessLayoutComponent,
    children: [
      {
        path: 'mis-reservas',
        canActivate: [clienteGuard],
        loadComponent: () => import('./pages/mis-reservas/mis-reservas.component')
          .then(module => module.MisReservasComponent)
      },
      {
        path: 'guardian/reservas',
        canActivate: [guardianGuard],
        loadComponent: () => import('./pages/panel-reservas/panel-reservas.component')
          .then(module => module.PanelReservasComponent)
      },
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

