import { AccessRule, SIGNED_IN, SUPER_ADMIN_ONLY } from '../../core/auth/access';
import { APP_PATHS, urlOf } from '../../core/app-paths';

export type NavigationIcon = 'complexes' | 'plans' | 'permissions';

export interface NavigationItem {
  readonly label: string;
  readonly url: string;
  readonly icon: NavigationIcon;
  /** Same rule as the route guard: the menu only shows what the user may open. */
  readonly access: AccessRule;
}

/** Side menu of «Pantallas» (PLATODO-27, «Navegación y permisos»). */
export const NAVIGATION_ITEMS: readonly NavigationItem[] = [
  { label: 'Complejos', url: urlOf(APP_PATHS.complexes), icon: 'complexes', access: SIGNED_IN },
  { label: 'Planes', url: urlOf(APP_PATHS.plans), icon: 'plans', access: SUPER_ADMIN_ONLY },
  {
    label: 'Permisos del operador',
    url: urlOf(APP_PATHS.operatorPermissions),
    icon: 'permissions',
    access: SUPER_ADMIN_ONLY,
  },
];
