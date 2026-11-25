# Cambios a documentos compartidos · PLATODO-18

Notas para pasar a los documentos del proyecto al preparar el release (regla de documentos compartidos).

## docs/architecture.md

En «Arquitectura del frontend», fila `app`: la pieza ya tiene proyecto (Vite 8, React 19, TypeScript 7, Vitest 5, oxlint). Organización inicial:

- `src/app/` (arranque, rutas y estilos globales), `src/shared/design/` (tokens y fuentes generados por `design-system/`), `src/shared/ui/` (componentes base `Button`, `Field`, `Card`) y `src/dev/` (catálogo `/dev/design`, solo en desarrollo).
- Iconos: Lucide (`lucide-react`), confirmado para `app`.
- Gestión de estado y acceso a la `api` siguen [POR DEFINIR]; no hay enrutador todavía (la única ruta extra es el catálogo de desarrollo).

## AGENTS.md

En «Comandos del proyecto», sección `app`, añadir:

```bash
pnpm lint                  # lint (oxlint)
```

## DESIGN.md

- Iconos: «[POR DEFINIR: librería concreta]» queda resuelto en `app` con Lucide (`lucide-react`).
- Siguen [POR DEFINIR] los tonos del secundario pulsado y del hover de peligro, y la sombra interior exacta del primario pulsado (en `app` se usa provisionalmente un borde interior `border-width-md` en `color-brand-navy`).
