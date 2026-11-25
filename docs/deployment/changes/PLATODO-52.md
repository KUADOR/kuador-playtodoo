# Cambios de despliegue · PLATODO-52

Ciclo de vida del complejo (alta, suspensión, archivado, reactivación y plazo de retención) y cambio de subdominio en la api.

## Variables

| Nombre | Pieza | Entornos | Secreto | Valor |
|---|---|---|---|---|
| `PLAYTODOO_TENANT_URL_PATTERN` (propiedad `playtodoo.tenant-url-pattern`) | api | pruebas, producción | no | URL base de la web de un complejo, con `{subdomain}`. Producción: `https://{subdomain}.playtodoo.com`. Pruebas: según su dominio raíz [POR DEFINIR: dominio raíz de pruebas]. Por defecto (local): `http://{subdomain}.localhost:8000`, el proxy de `infra/local` |
| `TENANCY_ARCHIVE_DEFAULTRETENTIONDAYS` (propiedad `tenancy.archive.default-retention-days`) | api | opcional | no | Días de archivado antes de la baja definitiva. Por defecto `45` (decisión del Equipo Kuador). Mínimo 1 |
| `TENANCY_SUBDOMAINS_RESERVED` (propiedad `tenancy.subdomains.reserved`) | api | opcional | no | Subdominios reservados, separados por coma. Por defecto la lista de ADR-012. `api` queda reservado siempre |

- La api responde la URL de `PLAYTODOO_TENANT_URL_PATTERN` cuando el propietario cambia el subdominio; si no se configura en un entorno desplegado, responde la URL local.

## Base de datos

- Sin migraciones nuevas: se usan `global.tenant` y `global.tenant_domain` (PLATODO-49) y `staff_user` de cada esquema (PLATODO-51).
- El alta de un complejo crea su esquema `t_<uuid>` con las migraciones de tenant. El usuario de base de datos de la api necesita permiso `CREATE` sobre la base (ya lo usa Flyway al arrancar).

## Qué hacer al desplegar

- **Producción y pruebas:** definir `PLAYTODOO_TENANT_URL_PATTERN`. Verificar con `curl -i https://api.<raíz>/api/v1/subdomains/www/availability` → `200` con `"reason": "RESERVED"`.
- **Vuelta atrás:** la versión anterior no expone estos endpoints; los complejos dados de alta con esta versión siguen resolviendo porque sus filas y esquemas son los mismos.
