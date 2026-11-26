import { Routes } from '@angular/router';
import { APP_PATHS } from './core/app-paths';
import { SUPER_ADMIN_ONLY } from './core/auth/access';
import { accessGuard, signedInGuard, signedOutGuard } from './core/auth/guards';
import { unsavedChangesGuard } from './core/navigation/unsaved-changes-guard';
import { PLAN_ID_PARAM } from './features/plans/plan-paths';
import { SECTION_HEADING_KEY } from './features/upcoming-section/section-heading';
import { devRoutes } from './dev-routes';

const TITLE_SUFFIX = ' · Backoffice de Kuador';

const loadPlanEditor = () =>
  import('./features/plans/plan-editor-page/plan-editor-page').then((m) => m.PlanEditorPage);

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
        // Replaced by P2 «Complejos» in PLATODO-90.
        path: APP_PATHS.complexes,
        title: `Complejos${TITLE_SUFFIX}`,
        data: { [SECTION_HEADING_KEY]: 'Complejos' },
        loadComponent: loadUpcomingSection,
      },
      {
        path: APP_PATHS.plans,
        title: `Planes${TITLE_SUFFIX}`,
        canActivate: [accessGuard(SUPER_ADMIN_ONLY)],
        loadComponent: () =>
          import('./features/plans/plans-page/plans-page').then((m) => m.PlansPage),
      },
      {
        path: APP_PATHS.planFields,
        title: `Campos de plan${TITLE_SUFFIX}`,
        canActivate: [accessGuard(SUPER_ADMIN_ONLY)],
        loadComponent: () =>
          import('./features/plans/plan-fields-page/plan-fields-page').then(
            (m) => m.PlanFieldsPage,
          ),
      },
      {
        path: APP_PATHS.newPlan,
        title: `Crear plan${TITLE_SUFFIX}`,
        canActivate: [accessGuard(SUPER_ADMIN_ONLY)],
        canDeactivate: [unsavedChangesGuard],
        loadComponent: loadPlanEditor,
      },
      {
        path: `${APP_PATHS.plans}/:${PLAN_ID_PARAM}`,
        title: `Editar plan${TITLE_SUFFIX}`,
        canActivate: [accessGuard(SUPER_ADMIN_ONLY)],
        canDeactivate: [unsavedChangesGuard],
        loadComponent: loadPlanEditor,
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
