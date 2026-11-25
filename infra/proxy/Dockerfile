# Proxy image for the deployed environments: Caddy plus the ACME DNS provider module needed to issue
# the wildcard certificate (*.<root domain>) through the DNS-01 challenge.
# Build: docker build --build-arg DNS_PROVIDER_MODULE=github.com/caddy-dns/<provider> -t platodo-proxy infra/proxy
ARG CADDY_VERSION=2.11.9

FROM caddy:${CADDY_VERSION}-builder-alpine AS builder
ARG DNS_PROVIDER_MODULE
RUN test -n "${DNS_PROVIDER_MODULE}" || { echo "DNS_PROVIDER_MODULE build arg is required" >&2; exit 1; } \
    && xcaddy build --with "${DNS_PROVIDER_MODULE}"

FROM caddy:${CADDY_VERSION}-alpine
COPY --from=builder /usr/bin/caddy /usr/bin/caddy
COPY Caddyfile routes.caddy /etc/caddy/
