# ADR-012 · Reglas de subdominio

- **Estado:** propuesto (lo aprueba una persona en la puerta del tablero).
- **Fecha:** 2026-11-25
- **Épica:** PLATODO-26 · Aislamiento de datos por complejo.
- **Detalla:** ADR-002 (tenant identificado por subdominio).
- **Contrato:** `contracts/tenancy.openapi.yaml` (etiquetas «Subdomains» y «Tenant»).

## Contexto

Cada complejo se identifica por su subdominio (ADR-002). El Equipo Kuador decidió que el sistema **sugiere** el subdominio al dar de alta el complejo, que el propietario puede **modificarlo** al registrarse y **después de creado**, y que el nuevo subdominio resuelve al mismo complejo. Faltaba fijar el formato, los reservados y qué pasa con el subdominio anterior.

## Decisión

1. **Formato.** Solo `a-z`, `0-9` y guion; entre 3 y 63 caracteres; no empieza ni termina en guion; no tiene dos guiones seguidos; no empieza con `xn--` (evita nombres IDN que imitan a otros). La entrada se normaliza a minúsculas. Error: `422 SUBDOMAIN_INVALID`.
2. **Reservados** (lista en configuración, error `422 SUBDOMAIN_RESERVED`): `www, api, admin, app, backoffice, static, cdn, assets, mail, smtp, ftp, status, docs, help, soporte, blog, dev, staging, test, localhost, playtodoo, kuador`.
3. **Sugerencia.** El nombre del complejo pasa a minúsculas, sin tildes, y lo que no es alfanumérico se cambia por un guion. Si el resultado está ocupado o reservado, se prueba `-2`, `-3`, etc. La sugerencia no reserva nada: al guardar se valida otra vez.
4. **Unicidad y concurrencia.** `global.tenant_domain` tiene el subdominio como clave primaria e índice único parcial `(tenant_id) WHERE is_current`. Dos solicitudes simultáneas por el mismo subdominio: una gana y la otra recibe `409 SUBDOMAIN_TAKEN`.
5. **Cambio.** El subdominio anterior deja de resolver de inmediato (`404 TENANT_NOT_FOUND`) y queda **retirado**: ningún otro complejo puede usarlo, para evitar suplantaciones (enlaces o marcadores viejos que llevarían a otro complejo). También quedan retirados tras la baja definitiva (ADR-013).
6. **Separado del esquema.** El subdominio no forma parte del nombre del esquema (ADR-010): cambiarlo solo actualiza `tenant_domain`.
7. **Dominio raíz configurable** por entorno (`playtodoo.root-domain`); en local, `{subdomain}.localhost`.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Liberar el subdominio anterior al cambiarlo | Otro complejo podría tomarlo y recibir a los clientes del anterior (suplantación) |
| Redirigir el subdominio anterior al nuevo | Útil, pero fuera del alcance de la épica; queda para más adelante |
| Permitir mayúsculas o caracteres internacionales | DNS no distingue mayúsculas y los IDN facilitan nombres engañosos |
| Que Kuador elija siempre el subdominio | Contradice la decisión del Equipo Kuador (lo sugiere el sistema y lo puede cambiar el propietario) |

## Consecuencias

- Los subdominios retirados se acumulan; si hace falta liberarlos se decidirá con una tarjeta propia.
- La lista de reservados se puede ampliar por configuración sin desplegar código.
