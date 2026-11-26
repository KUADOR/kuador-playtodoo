# Despliegue · PLATODO-82

Sin variables, secretos, servicios ni puertos nuevos.

## Qué cambia

- Migración global `V6__platform_roles_and_operator_capabilities.sql` (Flyway la aplica al arrancar la api, como las anteriores):
  - `platform_user.role`: `PLATFORM_OWNER` pasa a `SUPER_ADMIN`; el único otro valor permitido es `OPERATOR`.
  - `platform_user.display_name` (nuevo, obligatorio): las filas existentes toman la parte local de su correo.
  - Tablas nuevas `platform_capability`, `operator_capability` (todas las capacidades apagadas) y `capability_change`.

## Qué hacer en cada entorno

- **Local, staging y producción:** nada manual; la migración corre al desplegar.
- Después del despliegue, las personas de Kuador con sesión abierta deben **volver a iniciar sesión** en el backoffice: un token emitido antes lleva el rol `PLATFORM_OWNER`, que ya no existe, y recibe `403` hasta que vence (1 h como máximo).
- Si se quiere otro nombre visible para el dueño, actualizar `display_name` en `global.platform_user` (no hay pantalla de cuentas en esta épica).
