/** Texts of «Publicar» shared by P10 and P11 («Pantallas», PLATODO-27). */
export const PUBLISH_TEXTS = {
  confirmation: 'Se podrá asignar a complejos.',
  incomplete:
    'Al plan le faltan datos obligatorios para publicarse. Edítalo para completarlos y vuelve a intentarlo.',
} as const;

export function publishDialogHeading(planName: string): string {
  return `Publicar plan «${planName}»`;
}

export function publishedNotice(planName: string): string {
  return `Publicamos el plan «${planName}». Ya se puede asignar a complejos.`;
}
