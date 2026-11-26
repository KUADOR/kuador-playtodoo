# ADR-017 · Roles SUPER_ADMIN y OPERATOR con permisos por capacidades

- **Estado:** propuesto (lo aprueba una persona en la puerta del tablero).
- **Fecha:** 2026-11-26
- **Épica:** PLATODO-27 · Suscripciones y backoffice de Kuador.
- **Ajusta:** ADR-011 (solo `PLATFORM_OWNER` accedía a datos de complejos).
- **Contrato:** `contracts/platform-backoffice.openapi.yaml` (etiquetas «Me» y «Operator capabilities»).

## Contexto

El Equipo Kuador definió dos roles: `SUPER_ADMIN`, dueño con poder de hacer todo, y `OPERATOR`, con funciones que el `SUPER_ADMIN` configura. Por ahora: dar de alta complejos, registrar pagos, cambiar estados y acceder a datos de complejos con motivo; con el tiempo se agregarán otras.

## Decisión

1. **Roles.** `platform_user.role` admite `SUPER_ADMIN` y `OPERATOR`. Una migración de datos convierte `PLATFORM_OWNER` en `SUPER_ADMIN`, con prueba.
2. **Capacidades en código.** Un enum define las capacidades delegables: `COMPLEX_CREATE`, `SUBSCRIPTION_PAYMENT_RECORD`, `SUBSCRIPTION_STATUS_CHANGE` (marcar por vencer, suspender, reactivar y cancelar) y `TENANT_DATA_ACCESS` (soporte con motivo, ver y editar, solo en complejos `ACTIVE`). Se siembran por migración en `platform_capability`.
3. **Habilitación como dato.** `operator_capability` guarda si cada capacidad está habilitada para los operadores; todas empiezan apagadas. Cada cambio queda en `capability_change` (quién, cuándo, cuál, activada o desactivada) y publica `OperatorCapabilitiesChanged`.
4. **`CapabilityGuard`** (Identidad y acceso) se evalúa en cada solicitud: un permiso retirado falla en la siguiente con `403 CAPABILITY_REVOKED`. La api decide; el backoffice solo oculta (`GET /me` da las capacidades efectivas).
5. **Lectura del OPERATOR.** Listado, detalle, pagos e historial de los complejos, sin capacidad.
6. **Solo SUPER_ADMIN:** cambio de plan, planes y campos de plan, plazo de conservación, auditoría, capacidades y soporte en complejos suspendidos o cancelados (`403 FORBIDDEN` para el operador).
7. **Una capacidad nueva** se agrega en código (enum, guard y migración que la siembra apagada) y queda disponible para el `SUPER_ADMIN`.
8. Personal de un complejo y jugadores no acceden a `/api/v1/platform` (`403 FORBIDDEN`).

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Crear funciones desde la pantalla | Una función sin código que la respalde no protege nada |
| Permisos por usuario operador | No lo pidió el equipo y multiplica la configuración; las cuentas de operador quedan fuera de la épica |
| Guardar las capacidades en el token | Un permiso retirado seguiría valiendo hasta que venza el token |

## Consecuencias

- Una consulta global por solicitud para leer las capacidades habilitadas: barata y sin caché, para que el retiro aplique en la siguiente solicitud.
- Crear, invitar o dar de baja cuentas de operador queda fuera de esta épica.
