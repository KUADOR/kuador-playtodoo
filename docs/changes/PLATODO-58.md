# Cambios a documentos compartidos · PLATODO-58

Integración de extremo a extremo de la épica PLATODO-26. Notas para pasar a los documentos del proyecto al preparar el release.

## docs/architecture.md

En «Clientes», fila `movil`: reemplazar «Hoy funciona contra el mock del contrato (`InMemoryTenantApi`). El adaptador HTTP llega con la tarea de integración» (nota de PLATODO-57) por:

- `HttpTenantApi` (`lib/features/tenant/data/`) consulta `GET /api/v1/tenant` en el host del complejo con `dart:io` (sin dependencias nuevas). `200` → activo, `403 TENANT_UNAVAILABLE` → no disponible, `404 TENANT_NOT_FOUND` → no encontrado; sin conexión, más de 10 s o una respuesta fuera del contrato → «No pudimos comprobar el complejo».
- `TenantHosts` arma el host del complejo con `PLAYTODOO_TENANT_URL_PATTERN` (mismo valor que `playtodoo.tenant-url-pattern` de la api; por defecto `https://{subdomain}.playtodoo.com`).
- Hasta que exista la búsqueda de complejos (ADR-008), se entra a un complejo por su subdominio desde una pantalla solo de depuración (`/tenant-entry`).

En «Clientes», fila `app`: `TenantGate` entrega el complejo activo a la app con `useActiveTenant()` (contexto `ActiveTenantContext`). La portada muestra su nombre.

## movil/README.md

Agregar:

```bash
# Contra la api local (infra/local): el patrón apunta al proxy por subdominios
flutter run --dart-define='PLAYTODOO_TENANT_URL_PATTERN=http://{subdomain}.localhost:8000'

# Recorrido de extremo a extremo (lo usa infra/local/e2e/capture-screens.sh)
flutter drive --driver=test_driver/integration_test.dart \
  --target=integration_test/tenant_walkthrough_test.dart -d <dispositivo> --dart-define=…
```

`flutter test` no corre `integration_test/`: esas pruebas necesitan el entorno local levantado y un simulador o emulador.

## AGENTS.md

En «Comandos del proyecto», «Levantar en local»: reemplazar el `[POR DEFINIR]` por la referencia a `infra/local/README.md` (PLATODO-55) y agregar el recorrido `infra/local/e2e/tenancy-walkthrough.sh` (ver `infra/local/e2e/README.md`).
