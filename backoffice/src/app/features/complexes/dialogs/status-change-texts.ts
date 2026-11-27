import { ButtonVariant } from '../../../shared/ui/button/button';
import { AlertTone } from '../../../shared/ui/alert/alert';
import { formatDateTime } from '../../../core/format/date-format';
import { SUBSCRIPTION_STATUS_LOOKS } from '../complex-texts';
import { ComplexDetail, SubscriptionStatus } from '../complexes-api';

/** What «Cambiar estado» (P5) says and does for one target status. */
export interface StatusChangeOption {
  readonly target: SubscriptionStatus;
  readonly label: string;
  readonly description: string;
  /** Effect on access and data, shown before confirming. */
  readonly effect: string;
  readonly effectTone: AlertTone;
  readonly confirmLabel: string;
  readonly confirmVariant: ButtonVariant;
  /** Label of the button that closes the dialog without changing anything. */
  readonly closeLabel: string;
}

const CLOSE_LABEL = 'Cancelar';
const BLOCKED_ACCESS =
  'Nadie del complejo podrá entrar y los jugadores no lo verán ni podrán reservar.';

function suspendOption(): StatusChangeOption {
  return {
    target: 'SUSPENDED',
    label: SUBSCRIPTION_STATUS_LOOKS.SUSPENDED.label,
    description: 'Bloquea el acceso al complejo mientras se resuelve la suscripción.',
    effect: `${BLOCKED_ACCESS} Las reservas futuras quedan inactivas. Empieza el plazo de conservación: si no se reactiva antes de que venza, todos sus datos se borrarán. La fecha exacta se mostrará en el complejo.`,
    effectTone: 'error',
    confirmLabel: 'Suspender complejo',
    confirmVariant: 'danger',
    closeLabel: CLOSE_LABEL,
  };
}

function cancelOption(complex: ComplexDetail): StatusChangeOption {
  const runningDeadline =
    complex.purgeDueAt === null
      ? 'Empieza el plazo de conservación y, al vencer, todos sus datos se borrarán.'
      : `Se mantiene el plazo de conservación: todos sus datos se borrarán a partir del ${formatDateTime(complex.purgeDueAt)}.`;
  return {
    target: 'CANCELLED',
    label: SUBSCRIPTION_STATUS_LOOKS.CANCELLED.label,
    description: 'Termina la suscripción. No se puede deshacer.',
    effect: `${BLOCKED_ACCESS} La suscripción no se podrá reactivar. ${runningDeadline}`,
    effectTone: 'error',
    confirmLabel: 'Cancelar suscripción',
    confirmVariant: 'danger',
    closeLabel: 'Volver',
  };
}

function activeOption(complex: ComplexDetail): StatusChangeOption {
  if (complex.subscription.status === 'SUSPENDED') {
    return {
      target: 'ACTIVE',
      label: SUBSCRIPTION_STATUS_LOOKS.ACTIVE.label,
      description: 'Reactiva el complejo suspendido.',
      effect:
        'El personal recupera el acceso, el complejo vuelve a mostrarse y las reservas conservadas se activan.',
      effectTone: 'info',
      confirmLabel: 'Reactivar complejo',
      confirmVariant: 'primary',
      closeLabel: CLOSE_LABEL,
    };
  }
  return {
    target: 'ACTIVE',
    label: SUBSCRIPTION_STATUS_LOOKS.ACTIVE.label,
    description: 'La suscripción vuelve a estar al día.',
    effect: 'No cambia el acceso del complejo.',
    effectTone: 'info',
    confirmLabel: 'Marcar como activa',
    confirmVariant: 'primary',
    closeLabel: CLOSE_LABEL,
  };
}

function expiringOption(): StatusChangeOption {
  return {
    target: 'EXPIRING',
    label: SUBSCRIPTION_STATUS_LOOKS.EXPIRING.label,
    description: 'Avisa que la suscripción está por vencer.',
    effect: 'No cambia el acceso del complejo.',
    effectTone: 'info',
    confirmLabel: 'Marcar por vencer',
    confirmVariant: 'primary',
    closeLabel: CLOSE_LABEL,
  };
}

/** Options for the transitions the api allows now (`allowedTransitions`), in that order. */
export function statusChangeOptions(complex: ComplexDetail): StatusChangeOption[] {
  return complex.allowedTransitions.map((target) => {
    switch (target) {
      case 'ACTIVE':
        return activeOption(complex);
      case 'EXPIRING':
        return expiringOption();
      case 'SUSPENDED':
        return suspendOption();
      case 'CANCELLED':
        return cancelOption(complex);
    }
  });
}

export const STATUS_CHANGE_ERROR_MESSAGES = {
  TRANSITION_NOT_ALLOWED: 'Ese cambio de estado ya no está disponible. Revisa el estado actual.',
  RETENTION_EXPIRED: 'El plazo de conservación venció. Ya no se puede reactivar.',
} as const;
