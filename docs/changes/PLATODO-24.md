# Cambios a documentos compartidos · PLATODO-24

Notas para pasar a los documentos del proyecto al preparar el release (regla de documentos compartidos).

## DESIGN.md

El `DESIGN.md` del repositorio sigue siendo la propuesta v1 (verde #0F6B3E y acento ámbar). Los tokens (`design-system/tokens/`) y los componentes usan la Identidad visual v2 del tablero, derivada del logo oficial (navy #0B2F66, primario #0B4FB3, acento #4DB82A). Hay que reemplazar el archivo por la v2 del tablero. Actualizarlo con la identidad definitiva (Figma) es otra tarjeta.

Pendientes de identidad que encontró la revisión de paridad (se agregan a la lista de pendientes de DESIGN.md):

- Tokens de movimiento: duración del spinner y del pulso del esqueleto.
- Tokens de puntos de quiebre: hoy `768px` en el calendario de `app`.
- Sombra exacta del pulsado del primario: hoy `app` y `backoffice` usan sombras interiores distintas, ambas provisionales.
- Aspecto de la celda del mes en el calendario (fondo y marca de hoy), para unificar `movil` con `app` y `backoffice`.

## docs/architecture.md

Sin cambios.

## AGENTS.md

Sin cambios. Los comandos de `movil` siguen como los dejaron PLATODO-17 y PLATODO-22.

## README.md

Sin cambios.
