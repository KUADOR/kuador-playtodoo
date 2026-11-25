# Subdomain proxy (Caddy 2.11)

Reverse proxy in front of the api and the two webs. It routes by host (ADR-002, ADR-012) and is the
only entry point from the internet in the deployed environments.

| File | Use |
|---|---|
| `routes.caddy` | Host routing shared by every environment (`platodo_routes` snippet) |
| `Caddyfile.local` | Local: `http://{sub}.localhost:8000`, no TLS. Started by `infra/local/compose.yaml` |
| `Caddyfile` | Pruebas and producción: HTTPS with a wildcard certificate via ACME DNS-01 |
| `Dockerfile` | Deployed image: Caddy plus the DNS provider module (`--build-arg DNS_PROVIDER_MODULE=…`) |
| `test/smoke.sh` | Routing smoke test against echo upstreams (also run by `.github/workflows/proxy.yml`) |

## Routing

| Host | Upstream |
|---|---|
| `api.<root>` | api, global context |
| `admin.<root>` | backoffice |
| `<root>`, `www.<root>` | `404` until the root site exists |
| `{sub}.<root>` with `/api` or `/api/**` | api, tenant context |
| `{sub}.<root>`, any other path | app |
| Malformed subdomain (ADR-012 format), nested labels, unknown domains | `404` from the proxy |

## Forwarded headers

- The original `Host` reaches the upstream unchanged; the api resolves the tenant from it.
- Caddy sets `X-Forwarded-For`, `X-Forwarded-Proto` and `X-Forwarded-Host`. Values sent by a client are
  replaced unless the client is in `PLATODO_TRUSTED_PROXIES` (`test/smoke.sh` checks that a spoofed
  `X-Forwarded-Host` is replaced).
- Trusted proxies are configured per environment on both hops:
  - **Proxy** (`PLATODO_TRUSTED_PROXIES`): load balancers or CDNs in front of Caddy, as CIDRs separated by
    spaces, or `private_ranges`. Empty when Caddy is the edge (nobody is trusted).
  - **api** (Spring Boot 4.2, Tomcat): `SERVER_REQUEST_HEADER_FORWARDED_STRATEGY=web-server` and
    `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` with the proxy's network only (CIDRs separated by commas).
    Tomcat then honours `X-Forwarded-*` only from the proxy. The default trusts every private range,
    which is too broad outside local development. (`server.forward-headers-strategy` is deprecated
    since 4.2.)

## Variables

Values live in the deployment platform, never in the repo.

| Name | Used by | Secret | Example value |
|---|---|---|---|
| `PLATODO_ROOT_DOMAIN` | both Caddyfiles | no | `playtodoo.com`, `localhost` |
| `PLATODO_API_UPSTREAM` | both | no | `api:8080` |
| `PLATODO_APP_UPSTREAM` | both | no | `app:80` |
| `PLATODO_BACKOFFICE_UPSTREAM` | both | no | `backoffice:80` |
| `PLATODO_TRUSTED_PROXIES` | both | no | empty, `private_ranges`, `10.0.0.0/8` |
| `ACME_EMAIL` | `Caddyfile` | no | operations mailbox for ACME notices |
| `ACME_DNS_PROVIDER` | `Caddyfile` | no | provider module name, e.g. `cloudflare` |
| `ACME_DNS_API_TOKEN` | `Caddyfile` | **yes** | DNS API token limited to editing the zone's TXT records |

`ACME_DNS_PROVIDER` must match the module built into the image (`DNS_PROVIDER_MODULE`). The Caddyfile
passes the token as the provider's single argument, which fits token-based providers; others need a
`dns` block with their own fields.

## Certificates and storage

Caddy keeps certificates and ACME account keys in `/data`. Mount it on a persistent volume in each
deployed environment; otherwise every restart requests new certificates and can hit ACME rate limits.

## Commands

```bash
infra/proxy/test/smoke.sh                                    # routing smoke test (Docker)
docker run --rm -v "$PWD/infra/proxy:/config:ro" caddy:2.11.9-alpine caddy fmt /config/routes.caddy
docker build --build-arg DNS_PROVIDER_MODULE=github.com/caddy-dns/<provider> -t platodo-proxy infra/proxy
```
