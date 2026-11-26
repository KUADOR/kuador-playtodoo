import { Routes } from '@angular/router';
import { APP_PATHS, COMPLEX_PATHS } from './core/app-paths';
import { requiresCapability, SUPER_ADMIN_ONLY } from './core/auth/access';
import { accessGuard, signedInGuard, signedOutGuard } from './core/auth/guards';
import { SECTION_HEADING_KEY } from './features/upcoming-section/section-heading';
import { devRoutes } from './dev-routes';

const TITLE_SUFFIX = ' · Backoffice de Kuador';

const loadUpcomingSection = () =>
  import('./features/upcoming-section/upcoming-section-page').then((m) => m.UpcomingSectionPage);

export const routes: Routes = [
  ...devRoutes,
  {
    path: APP_PATHS.login,
    title: `Iniciar sesión${TITLE_SUFFIX}`,
    canActivate: [signedOutGuard],
    loadComponent: () => import('./features/login/login-page').then((m) => m.LoginPage),
  },
  {
    path: '',
    canActivate: [signedInGuard],
    loadComponent: () => import('./layout/shell/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: APP_PATHS.complexes },
      {
        path: APP_PATHS.complexes,
        children: [
          {
            path: '',
            pathMatch: 'full',
            title: `Complejos${TITLE_SUFFIX}`,
            loadComponent: () =>
              import('./features/complexes/list/complexes-page').then((m) => m.ComplexesPage),
          },
          {
            path: COMPLEX_PATHS.register,
            title: `Dar de alta complejo${TITLE_SUFFIX}`,
            canActivate: [accessGuard(requiresCapability('COMPLEX_CREATE'))],
            loadComponent: () =>
              import('./features/complexes/register/register-complex-page').then(
                (m) => m.RegisterComplexPage,
              ),
          },
          {
            path: `:${COMPLEX_PATHS.complexIdParam}`,
            title: `Detalle del complejo${TITLE_SUFFIX}`,
            loadComponent: () =>
              import('./features/complexes/detail/complex-detail-page').then(
                (m) => m.ComplexDetailPage,
              ),
            children: [
              {
                path: '',
                pathMatch: 'full',
                loadComponent: () =>
                  import('./features/complexes/detail/complex-summary-tab').then(
                    (m) => m.ComplexSummaryTab,
                  ),
              },
              {
                path: COMPLEX_PATHS.payments,
                loadComponent: () =>
                  import('./features/complexes/detail/complex-payments-tab').then(
                    (m) => m.ComplexPaymentsTab,
                  ),
              },
              {
                path: COMPLEX_PATHS.history,
                loadComponent: () =>
                  import('./features/complexes/detail/complex-history-tab').then(
                    (m) => m.ComplexHistoryTab,
                  ),
              },
            ],
          },
        ],
      },
      {
        // Replaced by P10 «Planes» in PLATODO-91.
        path: APP_PATHS.plans,
        title: `Planes${TITLE_SUFFIX}`,
        canActivate: [accessGuard(SUPER_ADMIN_ONLY)],
        data: { [SECTION_HEADING_KEY]: 'Planes' },
        loadComponent: loadUpcomingSection,
      },
      {
        path: APP_PATHS.operatorPermissions,
        title: `Permisos del operador${TITLE_SUFFIX}`,
        canActivate: [accessGuard(SUPER_ADMIN_ONLY)],
        loadComponent: () =>
          import('./features/operator-permissions/operator-permissions-page').then(
            (m) => m.OperatorPermissionsPage,
          ),
      },
      {
        path: APP_PATHS.forbidden,
        title: `Sin permiso${TITLE_SUFFIX}`,
        loadComponent: () =>
          import('./features/forbidden/forbidden-page').then((m) => m.ForbiddenPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
