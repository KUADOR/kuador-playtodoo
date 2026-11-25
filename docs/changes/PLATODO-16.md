# Cambios a documentos compartidos · PLATODO-16

Notas para pasar a los documentos del proyecto al preparar el release (regla de documentos compartidos).

## DESIGN.md

- El archivo del repositorio sigue en la propuesta v1 (verde #0F6B3E, ámbar, Inter). La Identidad visual vigente en el tablero es la **v2** (derivada del logo oficial), y es la que implementan `design-system/tokens`. Sincronizar DESIGN.md con la v2.
- Añadir que los valores viven en `design-system/tokens/*.json` y que los valores crudos de la identidad tienen nombre de token: `color-on-primary` (#FFFFFF), `color-disabled-bg` (#E5E7EB), `color-disabled-fg` (#4B5563), `color-disabled-border` (#9CA3AF), `color-occupied-bg` (#E5E7EB), `color-focus-halo` (#9CC0F5), `size-touch-min` (44), `size-content-max` (1200), `border-focus-ring` (2), `border-focus-offset` (2), `border-width-sm` (1), `border-width-md` (1,5) y `border-width-lg` (2).
- Pendiente 4 («Validar los contrastes con herramienta»): resuelto. `npm test` en `design-system/` valida 29 pares con la fórmula WCAG y todos cumplen AA (mínimo medido: 4,73:1 del borde `accent-strong` sobre `accent-tint`, que exige 3:1). El borde del secundario deshabilitado (`#9CA3AF` sobre blanco, 2,54:1) se reporta sin exigir, porque WCAG exime los controles inactivos.
- Valores que la identidad describe sin dato y quedan [POR DEFINIR] en `docs/components/button.md`: el «primary-tint más oscuro» del secundario pulsado y el «más oscuro» del hover de peligro.

## docs/architecture.md

En «Arquitectura del frontend», resolver el [POR DEFINIR] del sistema de diseño compartido: se comparten tokens, fuentes, fixtures y contratos desde `design-system/` (fuente única DTCG + Style Dictionary, salidas generadas y versionadas en cada pieza); cada framework implementa sus componentes de forma nativa (decisiones D1, D2, D3, D7 y D8 de PLATODO-15).

## README.md

Añadir `design-system/` como carpeta de herramientas (no es una pieza ni se despliega) con sus comandos: `npm run build`, `npm test` y `npm run tokens:check`.

## AGENTS.md

En «Comandos del proyecto», añadir:

```bash
# design-system (design-system/) · tokens y fuentes compartidas
npm run build          # generar tokens y copiar fuentes y fixtures a cada pieza
npm test               # pruebas y contraste WCAG
npm run tokens:check   # falla si lo generado no coincide con los tokens
```
