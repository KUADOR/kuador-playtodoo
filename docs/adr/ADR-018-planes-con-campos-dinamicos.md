# ADR-018 · Planes con campos básicos tipados, adicionales en JSONB e instantánea en la suscripción

- **Estado:** propuesto (lo aprueba una persona en la puerta del tablero).
- **Fecha:** 2026-11-26
- **Épica:** PLATODO-27 · Suscripciones y backoffice de Kuador.
- **Contrato:** `contracts/platform-backoffice.openapi.yaml` (etiquetas «Plans» y «Plan fields»).

## Contexto

Kuador crea los planes que se publican. El mínimo es nombre, precio, moneda, periodicidad y límites de canchas y usuarios, pero el equipo pidió un CRUD de los campos posibles para que los planes sean dinámicos.

## Decisión

1. **Campos básicos tipados** en `plan`: `name` (único, sin distinguir mayúsculas), `price numeric(19,4)`, `currency char(3)`, `periodicity`, `court_limit`, `user_limit`. Monedas y periodicidades permitidas salen de parámetros de plataforma ([POR DEFINIR: valores]).
2. **Campos adicionales** en `plan.custom_fields jsonb`, validados contra `plan_field_definition` (`key` estable, `label` único, tipo `TEXT`, `NUMBER` o `BOOLEAN` inmutable, `required`, origen `BASIC` o `CUSTOM`, `retired_at`). Los básicos se siembran con origen `BASIC`, son de solo lectura y no se retiran.
3. **Borrador y publicación.** Un borrador exige solo el nombre; publicar exige los básicos y los adicionales obligatorios no retirados (`422 PLAN_INCOMPLETE`). Despublicar o borrar planes queda fuera de alcance.
4. **Retirar un campo** deja de ofrecerlo y exigirlo, pero no borra valores existentes.
5. **Instantánea.** Al asignar un plan, `subscription.plan_snapshot jsonb` copia sus condiciones. Editar un plan publicado solo afecta a asignaciones nuevas.
6. Dinero como texto decimal en la API (`'120.00'`), nunca como número de punto flotante.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Tablas EAV para los adicionales | Más consultas y complejidad sin ganancia para pocos campos por plan |
| Todo en JSONB, incluidos los básicos | Pierde tipos y restricciones en lo que más se consulta |
| Propagar la edición del plan a quienes ya lo tienen | Cambiaría el precio de un complejo sin aviso |

## Consecuencias

- La validación de adicionales vive en el dominio de Planes y suscripciones, con pruebas por tipo.
- El listado de complejos muestra el nombre del plan desde la instantánea, sin unir con `plan`.
