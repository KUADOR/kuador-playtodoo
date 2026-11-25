/**
 * Where each piece of the repository receives its generated tokens and copied assets.
 * Paths are relative to the repository root so the same map serves the real build
 * (root = repo) and tokens:check (root = temporary directory).
 */
export const GENERATED_HEADER =
  'GENERATED – do not edit. Source: design-system/tokens. Regenerate with `npm run build` in design-system/.';

export const TOKEN_OUTPUTS = [
  { platform: 'app-css', dir: 'app/src/shared/design', file: 'tokens.css', format: 'playtodoo/css', nameTransform: 'name/kebab' },
  { platform: 'app-ts', dir: 'app/src/shared/design', file: 'tokens.ts', format: 'playtodoo/ts', nameTransform: 'name/camel' },
  { platform: 'backoffice-css', dir: 'backoffice/src/styles', file: 'tokens.css', format: 'playtodoo/css', nameTransform: 'name/kebab' },
  { platform: 'movil-dart', dir: 'movil/lib/core/design', file: 'tokens.g.dart', format: 'playtodoo/dart', nameTransform: 'name/camel' },
];

export const FONT_FACES = [
  { family: 'Nunito', dir: 'nunito', weight: 400, file: 'Nunito-Regular' },
  { family: 'Nunito', dir: 'nunito', weight: 600, file: 'Nunito-SemiBold' },
  { family: 'Nunito', dir: 'nunito', weight: 800, file: 'Nunito-ExtraBold' },
  { family: 'Inter', dir: 'inter', weight: 400, file: 'Inter-Regular' },
  { family: 'Inter', dir: 'inter', weight: 600, file: 'Inter-SemiBold' },
  { family: 'Inter', dir: 'inter', weight: 800, file: 'Inter-ExtraBold' },
];

export const FONT_LICENSE_FILE = 'OFL.txt';

/** Web pieces get woff2 files plus a generated fonts.css that references them relatively. */
export const WEB_FONT_TARGETS = [
  { cssDir: 'app/src/shared/design', fontsDir: 'app/src/shared/design/fonts' },
  { cssDir: 'backoffice/src/styles', fontsDir: 'backoffice/src/styles/fonts' },
];

/** Flutter only reads TrueType/OpenType files. */
export const NATIVE_FONT_TARGETS = [{ fontsDir: 'movil/assets/fonts' }];

export const CALENDAR_FIXTURE_TARGETS = [
  'app/src/shared/ui/booking-calendar/fixtures',
  'backoffice/src/app/shared/ui/booking-calendar/fixtures',
  'movil/test/shared/ui/booking_calendar/fixtures',
];
