import { PagoResultadoComponent } from './shared/pago-modal/pago-resultado.component';
import { Routes } from '@angular/router';
import { Component } from '@angular/core';
import { catalogoGuard, cuentaGuard, pagoGuard } from './guards/acceso.guard';
import { BusinessLayoutComponent } from './layouts/business-layout/business-layout.component';
import { DetalleCabanaComponent } from './pages/detalle-cabana/detalle-cabana.component';
import { CatalogoComponent } from './pages/catalogo/catalogo.component';
import { LoginComponent } from './pages/login/login.component';
import { guardianGuard } from './guards/guardian.guard';
import { clienteGuard } from './guards/cliente.guard';

@Component({ template: '<h1>Bienvenido</h1>' })
class InicioDuenoComponent {}

export const routes: Routes = [
  { path: 'pago/resultado', canActivate: [pagoGuard], component: PagoResultadoComponent },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: '',
    component: BusinessLayoutComponent,
    children: [
      {
        path: 'gestion',
        canActivate: [cuentaGuard],
        loadComponent: () => import('./pages/gestion/gestion.component').then((m) => m.GestionComponent),
      },
      { path: 'inicio-dueno', component: InicioDuenoComponent },
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
        canActivate: [catalogoGuard],
        component: CatalogoComponent
      },
      {
        path: 'catalogo/:id',
        canActivate: [catalogoGuard],
        component: DetalleCabanaComponent
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
