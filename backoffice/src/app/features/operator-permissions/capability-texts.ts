export interface CapabilityText {
  readonly label: string;
  readonly description: string;
}

/** Spanish texts of P13 «Permisos del operador» by capability key. */
const CAPABILITY_TEXTS: Readonly<Record<string, CapabilityText>> = {
  COMPLEX_CREATE: {
    label: 'Dar de alta complejos',
    description: 'Crear complejos nuevos y reintentar las altas incompletas.',
  },
  SUBSCRIPTION_PAYMENT_RECORD: {
    label: 'Registrar pagos',
    description: 'Registrar pagos de suscripción recibidos fuera de la plataforma.',
  },
  SUBSCRIPTION_STATUS_CHANGE: {
    label: 'Cambiar estados',
    description: 'Incluye marcar por vencer, suspender, reactivar y cancelar suscripciones.',
  },
  TENANT_DATA_ACCESS: {
    label: 'Acceder a datos de complejos con motivo',
    description:
      'Ver y editar datos de complejos activos, con motivo y auditoría. Nunca de suspendidos.',
  },
};

/**
 * Text of a capability. The list comes from the api, so a capability added later still shows up
 * (with the api description) until it gets its Spanish text here.
 */
export function capabilityText(key: string, apiDescription = ''): CapabilityText {
  return CAPABILITY_TEXTS[key] ?? { label: key, description: apiDescription };
}
