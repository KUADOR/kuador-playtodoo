# design-system

Single source of the Playtodoo design tokens (DTCG JSON) and the generator that writes them, in each piece's native format, into `app`, `backoffice` and `movil`. Development tooling only: it is not deployed nor part of any runtime.

## Commands

Requires Node.js 22 or later.

```bash
npm ci
npm run build         # generate tokens and copy fonts and calendar fixtures into every piece
npm test              # unit tests + WCAG contrast check of contrast-pairs.json
npm run tokens:check  # regenerate into a temp dir and fail if committed outputs differ
```

`npm run build` is idempotent: running it twice produces byte-identical files. Commit its output together with the token change.

## Layout

| Path | Content |
|---|---|
| `tokens/*.json` | DTCG tokens (`$value`, `$type`): color, typography, size, shadow. Values from DESIGN.md v2 |
| `contrast-pairs.json` | Foreground/background pairs with their kind: `text` (4.5:1), `component` (3:1) or `exempt` (reported only) |
| `assets/fonts/` | Nunito and Inter 400/600/800, `woff2` (latin subset, for web) and `ttf` (for Flutter), OFL licence |
| `fixtures/calendar/` | Shared `BookingCalendar` cases with their expected output |
| `docs/components/` | Component contracts: `calendar.md`, `button.md`, `field.md`, `card.md` |
| `build/` | Style Dictionary setup, output formats, value serializers and output targets |
| `scripts/` | `build`, `copy-assets`, `contrast-check`, `tokens-check` |
| `test/` | `node:test` suites; `test/support/calendar-reference.js` recomputes fixture expectations |

## Generated outputs

| Piece | Files |
|---|---|
| `app` | `src/shared/design/tokens.css`, `tokens.ts`, `fonts.css`, `fonts/*.woff2`; fixtures in `src/shared/ui/booking-calendar/fixtures/` |
| `backoffice` | `src/styles/tokens.css`, `fonts.css`, `fonts/*.woff2`; fixtures in `src/app/shared/ui/booking-calendar/fixtures/` |
| `movil` | `lib/core/design/tokens.g.dart`, `assets/fonts/*.ttf`; fixtures in `test/shared/ui/booking_calendar/fixtures/` |

Never edit generated files: they start with a `GENERATED` header and `tokens:check` fails on any manual change.

## Naming contract

| DTCG | CSS | TypeScript | Dart |
|---|---|---|---|
| `color.primary` | `--color-primary` | `tokens.colorPrimary` | `AppTokens.colorPrimary` |
| `space.4` | `--space-4` | `tokens.space4` | `AppTokens.space4` |
| `shadow.card` | `--shadow-card` | `tokens.shadowCard` | `AppTokens.shadowCard` (`List<BoxShadow>`) |

Dimensions are authored in `px`; Dart receives logical pixels as `double`. Dart font families carry only the first family; Flutter falls back to system fonts.

## Changing a token

1. Edit `tokens/*.json` (a visual identity change needs an approved card).
2. `npm run build && npm test`.
3. Commit the token change and the regenerated files of every piece together.
