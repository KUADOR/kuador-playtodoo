import { Routes } from '@angular/router';

/** Development-only routes. See dev-routes.ts for how they are excluded from production. */
export const devRoutes: Routes = [
  {
    path: 'design',
    title: 'Componentes base · Playtodoo',
    loadComponent: () => import('./dev/design-catalog/design-catalog').then((m) => m.DesignCatalog),
  },
];
