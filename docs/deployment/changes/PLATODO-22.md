# Cambios de despliegue · PLATODO-22

## movil

- Se crea el proyecto Flutter de `movil` para Android e iOS, con el identificador **com.kuador.playtodoo** (`applicationId` en Android y `PRODUCT_BUNDLE_IDENTIFIER` en iOS). Una vez publicada la app en las tiendas, ese identificador ya no se puede cambiar.
- Dependencias nuevas de la app, de pub.dev: `material_ui` 1.11.1 y `lucide_icons_flutter` 4.0.3.
- Con `pubspec.yaml` presente, `.github/workflows/movil.yml` (PLATODO-17) ahora sí ejecuta `flutter analyze` y `flutter test`.
- No hay variables, secretos, puertos ni dominios nuevos. No se publica en las tiendas ni se firma la app: queda fuera de esta tarea, [POR DEFINIR: cuentas de Google Play y App Store, y firma].

## Vuelta atrás

Revertir el PR. No hay estado que restaurar.
