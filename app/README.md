# Playtodoo Kuador · app

Pieza **app** del proyecto PLATODO: web del complejo. Stack: React · Vite · TypeScript.

Requiere Node.js 22 o superior y pnpm (versión fijada en `packageManager`).

| Compilar | Probar | Levantar | Lint |
|---|---|---|---|
| `pnpm build` | `pnpm test` | `pnpm dev` | `pnpm lint` |

## Structure

| Path | Content |
|---|---|
| `src/main.tsx` | Entry point: loads `fonts.css`, `tokens.css` and the global styles once |
| `src/app/` | App shell, routing and global styles |
| `src/shared/design/` | Design tokens and fonts **generated** by `design-system/` (never edit by hand) |
| `src/shared/ui/` | Base components (`Button`, `Field`, `Card`). Styles use only `var(--…)` tokens; `styles.test.ts` fails on color literals |
| `src/dev/design/` | Component catalog at `/dev/design`, only in development builds (`import.meta.env.DEV`) |
| `src/test/` | Test setup (Vitest + Testing Library + jest-dom) and the axe helper |

Component contracts and usage: `design-system/docs/components/*.md`.
