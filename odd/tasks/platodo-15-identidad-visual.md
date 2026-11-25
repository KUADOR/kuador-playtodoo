# Seguimiento: Identidad visual (tokens y componentes base)

Épica PLATODO-15. Este archivo cierra el seguimiento con la evidencia de la tarea final (PLATODO-24). Historia, alcance y decisiones: documento «Seguimiento» v3 del tablero; diseño: `docs/solution/platodo-15-identidad-visual.md`.

## Clasificación
- **Comprensión:** media-alta. Todas las decisiones de producto quedaron respondidas por el Equipo Kuador.
- **Riesgo:** medio. Transversal a las tres apps; sin cambios en la `api` ni en datos de complejos.
- **Revisión:** por pieza y con evidencia visual (capturas por componente y estado).
- **Estimación:** 13 story points (subida por el arquitecto de 8 a 13).

## Decisiones tomadas (Equipo Kuador)
1. Los tokens salen del Figma y del logo oficial; DESIGN.md v2 se derivó del logo y el Figma queda pendiente de exportar.
2. Apps objetivo: `app` (React), `backoffice` (Angular) y `movil` (Flutter).
3. `backoffice` ve el calendario de otros complejos en solo lectura.
4. Calendario con vistas diaria, semanal y mensual.
5. Se aceptan diferencias propias de cada plataforma si están documentadas.
6. Duración de franja configurable por complejo (el calendario la recibe como dato).
7. (PLATODO-21, opción A) La vista mensual muestra por día un resumen (`N libres` o `Sin horarios`) y lleva a la diaria; los estados de franja con el mismo aspecto se exigen en las vistas diaria y semanal.

## Tareas
- [x] PLATODO-16 · Fuente única de tokens, generador, contraste, fixtures y contrato del calendario (#1)
- [x] PLATODO-17 · CI de `design-system` y comandos de `movil` (#2)
- [x] PLATODO-18 · `app`: tokens y componentes base (#4)
- [x] PLATODO-19 · `app`: calendario de reservas (#6)
- [x] PLATODO-20 · `backoffice`: tokens y componentes base (#5)
- [x] PLATODO-21 · `backoffice`: calendario solo lectura (#7)
- [x] PLATODO-22 · `movil`: tema, tokens y componentes base (#8)
- [x] PLATODO-23 · `movil`: calendario de reservas (#9)
- [x] PLATODO-24 · Paridad entre apps, documentación y evidencia

## Estado

### Implementado
- Fuente única `design-system/tokens/*.json` (DTCG) y generador Style Dictionary hacia `app` (CSS + TS), `backoffice` (CSS) y `movil` (Dart), con `tokens:check` y chequeo de contraste WCAG en CI.
- `Button`, `Field`, `Card` y `BookingCalendar` en las tres piezas con un contrato común (`design-system/docs/components/*.md`) y fixtures compartidos del calendario.
- PLATODO-24: dos desvíos de paridad corregidos en `movil`:
  - `AppButton` en «cargando» se veía gris, como deshabilitado. Ahora conserva los colores de su variante, como en `app` y `backoffice`.
  - El check de `AppCard` seleccionada ahora va sobre un círculo `color-primary`, igual que en web.
  - Con movimiento reducido, el indicador de «cargando» queda quieto como un arco de tres cuartos, igual que el icono de web (antes se congelaba como un arco mínimo).
  - El catálogo de `movil` muestra ahora los mismos estados que los de web: cada variante de botón en normal, cargando y deshabilitado, y las cinco variantes de tarjeta más seleccionada y cargando (devolución de QA).
  Todos los cambios tienen pruebas nuevas.
- PLATODO-24: documentación final de los cuatro componentes (uso, propiedades, estados, ejemplos por pieza y tabla completa de diferencias aceptadas por plataforma con su motivo).

### Verificado

| Criterio | Cómo | Evidencia |
|---|---|---|
| 1 · Tokens en un único lugar | Cada token tiene nombre y valor solo en `design-system/tokens/`; las piezas reciben archivos generados con cabecera `GENERATED` y `tokens:check` falla ante cambios manuales | `npm run tokens:check` → «Generated files are up to date.» |
| 2 · Cambio de token sin tocar componentes | `color.primary` de `#0B4FB3` a `#8E24AA` (valor de prueba), `npm run build`, recompilación de `app` (`pnpm build`), `backoffice` (`npm run build`) y `movil` (`flutter analyze`). El diff tocó solo el JSON y los 4 archivos generados (ningún componente). Luego se revirtió y `tokens:check` volvió a quedar limpio | `docs/evidence/platodo-24/token-change-diffstat.txt`, `token-before-*.png` y `token-after-*.png` (botón, campo, tarjeta y calendario, las tres apps lado a lado) |
| 13 · Documentación con uso, estados y ejemplos | Los cuatro documentos de `design-system/docs/components/` tienen uso, propiedades, estados, ejemplos en React, Angular y Flutter, y diferencias por plataforma | `button.md`, `field.md`, `card.md`, `calendar.md` |
| 14 · Mismos tokens y comportamiento | Capturas de cada componente y estado en las tres apps a 390 px de ancho, lado a lado. Auditoría de valores sueltos (abajo) | `docs/evidence/platodo-24/parity-button.png`, `parity-field.png`, `parity-card.png`, `parity-calendar.png` |
| 15 · Compilación y pruebas en verde | Corrido en local sobre la rama de PLATODO-24 (abajo) | Salidas resumidas en «Evidencias» |

Los criterios 3 a 12 los verificaron sus tareas: PLATODO-18 a 23, con capturas en `docs/evidence/platodo-18` a `platodo-21` y `docs/screenshots/platodo-22` y `platodo-23`.

**Auditoría «solo tokens»** (colores en hex, `rgb()`, `px`/`rem` y duraciones en estilos de `shared/ui`; `Color(0x…)`, `Colors.` y números sueltos en `movil/lib/shared` y `lib/core/design`):
- No hay colores, tipografías, radios, sombras ni espaciados fuera de los tokens en ninguna de las tres piezas.
- Quedan valores que no son de identidad y no tienen token en DESIGN.md:
  - duración de las animaciones (giro del spinner `1s`, pulso del esqueleto `1.5s`);
  - el punto de quiebre `768px` del calendario en `app`;
  - medidas de maquetación del calendario en `backoffice` (`--calendar-today-dot`, `--calendar-slot-column-min`, `--calendar-month-min-width`), ya con nombre como propiedades CSS locales;
  - el `1px` del texto solo para lectores;
  - `elevation: 0` del tema de Flutter.

  Se aceptan así hasta que la identidad defina tokens de movimiento y de puntos de quiebre.

### Pendiente
- **Figma:** exportar del Figma los valores definitivos (hex, tipografía, radios, sombras). El Figma no se puede leer desde el tablero, así que el criterio 1 se cumple contra el logo oficial y DESIGN.md v2, y la parte «coincide con el Figma» queda pendiente. El ajuste solo toca `design-system/tokens/` y se regenera.
- **Celda del mes del calendario:** en `movil` las celdas con franjas libres llevan fondo `color-accent-tint` y hoy se marca con borde `color-primary`; en `app` y `backoffice` las celdas van en blanco. No es una diferencia de plataforma. [POR DEFINIR: aspecto de la celda del mes, lo decide Diseño UI/UX]. Está anotado en `calendar.md`.
- **Pendientes de identidad que ya estaban:**
  - pulsado del secundario y hover de peligro;
  - sombra exacta del pulsado del primario: hoy `app` y `backoffice` usan sombras interiores distintas, ambas provisionales;
  - tokens de movimiento y de puntos de quiebre;
  - logo en blanco y símbolo «P».
- **DESIGN.md:** el archivo del repositorio sigue siendo la propuesta v1 (verde y ámbar); los tokens usan la v2 del tablero. Actualizarlo es otra tarjeta (fuera de alcance). Queda anotado en `docs/changes/PLATODO-24.md`.
- **Criterio 7:** el Product Owner ajusta el texto según la decisión 7.
- **Piezas del tablero:** registrar en la pieza `movil` los comandos `flutter analyze` y `flutter test` (Flutter 3.50.1).

## Evidencias
- Paridad lado a lado (390 px; `app` · `backoffice` · `movil`): `docs/evidence/platodo-24/parity-{button,field,card,calendar}.png`.
- Cambio de token (criterio 2): `docs/evidence/platodo-24/token-before-*.png`, `token-after-*.png` y `token-change-diffstat.txt`.
- Corridas locales en la rama `feature/PLATODO-24-…` (Node 22, Flutter 3.50.1):
  - `design-system`: `npm run tokens:check` sin diferencias; `npm test` pasa (pruebas y contraste).
  - `app`: `pnpm lint` sin avisos; `pnpm build` correcto; `pnpm test` 132/132.
  - `backoffice`: `npx prettier --check src` correcto; `npm run build` correcto; `npm test -- --watch=false` 95/95.
  - `movil`: `flutter analyze` sin problemas; `flutter test` 90/90 (83 antes, más 7 nuevas de paridad).
- CI de `develop` en verde para `design-system`, `app`, `backoffice` y `movil` en los merges de PLATODO-19 a 23. La del PR de PLATODO-24 queda enlazada en el PR.
- Capturas de las tareas anteriores: `docs/evidence/platodo-18`, `platodo-19`, `platodo-20`, `platodo-21`, `docs/screenshots/platodo-22`, `platodo-23`.
