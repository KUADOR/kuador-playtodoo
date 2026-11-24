# Playtodoo Kuador

Playtodoo Kuador

Proyecto **PLATODO** del tablero. Los issues, las etapas y las aprobaciones viven en el tablero; este repo guarda el código y los documentos.

## Ramas

- `develop`: rama de trabajo. Cada issue va en su rama (`feature/PLATODO-<número>-<nombre>`) y entra por Pull Request con el CI en verde.
- `main`: lo publicado. Solo entra desde `develop` por Pull Request aprobado por una persona.

## Agentes

Los agentes trabajan desde el cliente de IA de cada persona conectado al tablero. Ver `AGENTS.md`.

## Piezas

| Pieza | Stack | Dónde | Compilar | Probar | Levantar |
|---|---|---|---|---|---|
| api | Java 25 · Spring Boot 4 | `api/` | `./mvnw -B package` | `./mvnw -B verify` | `./mvnw spring-boot:run` |
| backoffice | Angular · TypeScript | `backoffice/` | `npm run build` | `npm test -- --watch=false` | `npm start` |
| app | React · Vite · TypeScript | `app/` | `pnpm build` | `pnpm test` | `pnpm dev` |
| movil | Flutter | `movil/` | — | — | — |

## Documentos

- [Reglas para agentes](AGENTS.md)
- [Arquitectura](docs/architecture.md)
- [Despliegue](docs/deployment.md)
- [Glosario](docs/glossary.md)
- [Identidad visual](DESIGN.md)
- [Producto](PRODUCT.md)
