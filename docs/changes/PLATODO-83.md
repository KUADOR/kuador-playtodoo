# Cambios a documentos compartidos · PLATODO-83

## docs/architecture.md

En la sección «Módulos», fila «Planes y suscripciones», agregar la estructura que suma PLATODO-83 (ADR-018):

```
com.kuador.platodo.api.subscriptions/
  domain/                        Plan (borrador y publicación), PlanContent, PlanDraft, PlanContentRules
                                 (validación de básicos y adicionales), PlanFieldDefinition (clave, tipo inmutable,
                                 retiro), PlanFieldType, PlanFieldOrigin, PlanStatus, PlanOptions, FieldError
  application/                   ListPlans, GetPlan, CreatePlan, UpdatePlan, PublishPlan, GetPlanOptions,
                                 ListPlanFields, CreatePlanField, UpdatePlanField, RetirePlanField
  application/port/out/          Plans, PlanFieldDefinitions, PlanOptionsSource
  infrastructure/persistence/global/  JpaPlans, JpaPlanFieldDefinitions, PlatformParameterPlanOptions
  infrastructure/web/            PlanController (/plans, /plans/{id}, /plans/{id}/publication, /plan-options),
                                 PlanFieldController (/plan-fields, /plan-fields/{id}, /plan-fields/{id}/retirement),
                                 PlansProblemHandler
```

- **Autorización:** cada caso de uso llama a `CapabilityGuard` (Identidad y acceso). Lectura de planes publicados y de `/plan-options`: cualquier usuario de plataforma; lo demás, solo SUPER_ADMIN.
- **Unicidad:** nombres de plan y etiquetas de campo únicos sin distinguir mayúsculas, y claves de campo únicas, garantizados por índices únicos (`plan_name_unique`, `plan_field_definition_label_unique`, `plan_field_definition_key_unique`); el adaptador traduce la violación a `409 PLAN_NAME_TAKEN` o `409 FIELD_NAME_TAKEN`, así dos solicitudes simultáneas nunca pasan las dos.
- **Concurrencia optimista:** `PUT /plans/{id}` y `PUT /plan-fields/{id}` bloquean la fila (`PESSIMISTIC_WRITE`) y comparan `version`.
- **Parámetros de plataforma:** tabla global `platform_parameter` (clave, valor `jsonb`, descripción), creada en V7 con `plan.currencies` y `plan.periodicities` vacíos [POR DEFINIR]. Las tareas siguientes suman allí sus claves (`retention.default-days`, `audit.retention-days`, `history.retention-days`, `support.session-minutes`). Planes y suscripciones solo lee `plan.*`.
- **Migración global V7:** `platform_parameter`, `plan_field_definition` (básicos sembrados con origen `BASIC`) y `plan`.

## docs/glossary.md

Agregar:

- **Campo básico (de plan):** nombre, precio, moneda, periodicidad, límite de canchas y límite de usuarios. Tipados, de solo lectura y obligatorios para publicar.
- **Campo adicional (de plan):** campo que crea el SUPER_ADMIN, de tipo texto, número o sí/no (inmutable). Se puede retirar: deja de ofrecerse y exigirse, pero los valores que ya tienen los planes se conservan.

## contracts/platform-backoffice.openapi.yaml

Para que lo revise el arquitecto (no se cambió el contrato en esta tarea):

1. **Códigos de `FieldError`.** La api usa, además de los del ejemplo de la descripción: `INVALID_FORMAT` (precio que no cumple `Money`, o etiqueta sin letras ni dígitos de la que no se puede derivar `key`) y `NEGATIVE` (`courtLimit` o `userLimit` menor que 0). Propuesta: sumarlos a la lista de ejemplos de `FieldError.code`.
2. **Campos retirados al editar un plan.** «Retirar no borra valores»: `PUT /plans/{id}` conserva siempre los valores de campos retirados aunque no vengan en `customFields`, y acepta que vengan con el mismo valor guardado (un cliente que reenvía lo que leyó). Otro valor responde `422 VALIDATION_FAILED` con `NOT_ALLOWED`. Propuesta: decirlo en la descripción de `updatePlan`.
3. **Valor vacío de un adicional.** `null` o un texto en blanco en `customFields` quita el valor. Propuesta: decirlo en `CustomFieldValues`.
4. **Choque de clave.** Si la clave derivada de una etiqueta nueva ya existe (p. ej. «Support hours» y «Support-hours»), responde `409 FIELD_NAME_TAKEN`. Propuesta: mencionarlo en `createPlanField`.
5. **Texto de un adicional.** Máximo 500 caracteres (`TOO_LONG`). No está en el contrato; propuesta: `maxLength` en la descripción de `CustomFieldValues`.
