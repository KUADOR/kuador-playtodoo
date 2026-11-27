# Recorrido del backoffice contra la api real (corrida 1795790711)

Backoffice servido sin mock (`npm run start:api`) en `http://admin.localhost:41000`. 2026-11-27T14:45:12Z.

| # | Paso | Resultado |
|---|---|---|
| 1 | Inicio de sesión del SUPER_ADMIN contra la api real | OK |
| 2 | P2 Complejos: listado real con plan, estado y último pago (CA19) | OK |
| 3 | P4 Detalle del complejo | OK |
| 4 | P4 Pagos (CA7) | OK |
| 5 | P6 Historial de plan y estado (CA5, CA6) | OK |
| 6 | P8 Acceso con motivo: abre la sesión de soporte (CA13) | OK |
| 7 | P9 Vista de soporte en el host del complejo con X-Support-Session | OK |
| 8 | P7 Auditoría con persona, motivo y resultado (CA13) | OK |
| 9 | P10 Planes | OK |
| 10 | P12 Campos de plan (CA4) | OK |
| 11 | P13 Permisos del operador (CA16) | OK |
| 12 | Sin fallas de red, CORS, 5xx ni errores de consola | OK |

**Resultado: los 12 pasos dieron lo esperado.**
