# Despliegue de Playtodoo Kuador

Documento vivo. Cada Pull Request que cambia cómo se despliega deja su nota en `docs/deployment/changes/<tarjeta>.md`;
al preparar cada release esas notas se pasan aquí (y a «Cambios») y se borran.

## Entornos

| Entorno | Rama | Dónde corre | URL |
|---|---|---|---|
| Pruebas | develop | [servidor o plataforma] | [https://…] |
| Producción | main | [servidor o plataforma] | [https://…] |

## Infraestructura

[Servidor (proveedor, sistema operativo, tamaño), plataforma (Dokploy, Docker Compose…), proxy y certificados.]

## Piezas

| Pieza | Cómo se construye | Puerto | Dominio por entorno |
|---|---|---|---|
| [pieza] | [imagen Docker o build] | [puerto] | [pruebas / producción] |

## Dependencias

| Servicio | Versión | Para qué | Cómo se levanta | Datos que guarda |
|---|---|---|---|---|
| [ej. PostgreSQL] | [18] | [qué pieza lo usa] | [contenedor, servicio administrado] | [volumen y respaldo] |

## Variables y secretos

Solo nombres: los valores nunca van en este documento ni en el repo.

| Nombre | Pieza | Entornos | Dónde vive | Secreto |
|---|---|---|---|---|
| [NOMBRE] | [pieza] | [pruebas, producción] | [Dokploy, secretos de GitHub…] | [sí / no] |

## Paso a paso

### Primera vez

1. [Preparar el servidor]
2. [Crear las aplicaciones y dependencias]
3. [Cargar variables y secretos]
4. [Primer despliegue y verificación]

### Cada despliegue

[Qué pasa al fusionar en develop y en main.]

## CI/CD

[Workflow que construye, publica y despliega cada rama en su entorno.]

## Respaldos y vuelta atrás

[Qué se respalda, cada cuánto, dónde, y cómo volver a la versión anterior.]

## Verificación

[Cómo comprobar que cada entorno quedó bien: salud, logs, una prueba de punta a punta.]

## Cambios

| Fecha | Qué cambió | Tarjeta |
|---|---|---|
| 2026-11-24 | Primera versión | — |
