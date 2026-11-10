import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  { path: 'catalogo', renderMode: RenderMode.Client },
  { path: 'inicio-dueno', renderMode: RenderMode.Client },
  { path: 'gestion', renderMode: RenderMode.Client },
  { path: 'pago/resultado', renderMode: RenderMode.Client },
  { path: 'mis-reservas', renderMode: RenderMode.Client },
  {
    path: 'guardian/reservas',
    renderMode: RenderMode.Client
  },
  {
    path: 'catalogo/:id',
    renderMode: RenderMode.Client
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender
  }
];
