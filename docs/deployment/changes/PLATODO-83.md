# Despliegue · PLATODO-83

Sin variables, secretos, servicios ni puertos nuevos.

## Qué cambia

- Migración global `V7__create_plans_plan_fields_and_platform_parameter.sql` (Flyway la aplica al arrancar la api):
  - Tabla `platform_parameter` (parámetros de plataforma, valor `jsonb`) con `plan.currencies` y `plan.periodicities`, **vacíos**: [POR DEFINIR: monedas y periodicidades permitidas].
  - Tabla `plan_field_definition` con los seis campos básicos sembrados (`name`, `price`, `currency`, `periodicity`, `courtLimit`, `userLimit`; origen `BASIC`).
  - Tabla `plan` (borrador o publicado; adicionales en `custom_fields jsonb`).

## Qué hacer en cada entorno

- **Local, staging y producción:** la migración corre sola al desplegar.
- Mientras `plan.currencies` y `plan.periodicities` estén vacíos, ningún plan puede tener moneda ni periodicidad y, por lo tanto, no se puede publicar. Cuando el equipo Kuador defina los valores, cargarlos en cada entorno (no hay pantalla de parámetros en esta épica). Ejemplo con valores ilustrativos, no decididos:

  ```sql
  UPDATE global.platform_parameter SET value = '["USD"]' WHERE key = 'plan.currencies';
  UPDATE global.platform_parameter SET value = '["MONTHLY", "YEARLY"]' WHERE key = 'plan.periodicities';
  ```

  El cambio aplica en la siguiente solicitud, sin reiniciar la api. Las monedas son códigos ISO 4217 de tres letras mayúsculas.
