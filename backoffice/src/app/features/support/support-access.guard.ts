import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { COMPLEX_PATHS, complexDetailUrl } from '../../core/app-paths';
import { SupportAccess } from './support-access';

/**
 * P9 opens only with a support session for that same complex, opened with a reason in P8. A
 * direct address, a reload or another complex goes back to its P4.
 */
export const supportAccessGuard: CanActivateFn = (route) => {
  const complexId = route.paramMap.get(COMPLEX_PATHS.complexIdParam) ?? '';
  return (
    inject(SupportAccess).activeFor(complexId) !== null ||
    inject(Router).parseUrl(complexDetailUrl(complexId))
  );
};
