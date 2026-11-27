import { APP_PATHS, urlOf } from '../../core/app-paths';

export const PLANS_URL = urlOf(APP_PATHS.plans);
export const NEW_PLAN_URL = urlOf(APP_PATHS.newPlan);
export const PLAN_FIELDS_URL = urlOf(APP_PATHS.planFields);

/** Route parameter of P11 when editing. */
export const PLAN_ID_PARAM = 'planId';

export function planUrl(planId: string): string {
  return `${PLANS_URL}/${encodeURIComponent(planId)}`;
}
