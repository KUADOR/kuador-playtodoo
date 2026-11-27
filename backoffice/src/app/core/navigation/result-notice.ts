import { NavigationBehaviorOptions, Router } from '@angular/router';

const RESULT_NOTICE_KEY = 'resultNotice';

/**
 * «Éxito: aviso de resultado en la pantalla de destino» («Pantallas», «Reglas comunes»). The text
 * travels in the navigation state, so it is shown once and not again after a reload.
 */
export function withResultNotice(text: string): NavigationBehaviorOptions {
  return { state: { [RESULT_NOTICE_KEY]: text } };
}

/** The result notice of the navigation in progress; read it while the destination is created. */
export function resultNoticeOf(router: Router): string | null {
  const value = router.currentNavigation()?.extras.state?.[RESULT_NOTICE_KEY];
  return typeof value === 'string' ? value : null;
}
