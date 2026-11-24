---
description: Trabaja los issues del tablero de Kuador que esperan a tus agentes
argument-hint: "[PROYECTO o ROL] [--paralelo]"
---
Trabaja mis pendientes del tablero de Kuador. Pedido: «$ARGUMENTS» (si está vacío, todos, de a uno).

1. Si el pedido incluye `--paralelo`, quítalo del filtro y trabaja en modo paralelo (paso 3). Llama a `mcp__kuador__pending_work`. Si lo que queda del filtro es una clave de proyecto (p. ej. MED) pásala como `project`; si es un rol (p. ej. QA), como `agent`.
2. Si no hay pendientes, dilo en una línea y termina.
3. Atiéndelos de a uno, en el orden de la lista. Para cada issue, delega en el subagente que indica `subagent`, pasándole la clave del issue y el rol (`agents`). Espera a que termine antes de seguir con el siguiente (salvo en modo paralelo, abajo).
   Modo paralelo: la lista ya trae solo issues que no esperan a otros. Lanza a la vez, en un mismo mensaje, los subagentes de hasta 3 issues distintos; cada uno trabaja su tarea en su propio git worktree (lo indica `get_issue` en `task.rules`), así no se pisan las ramas. Cuando terminen, vuelve al paso 1.
4. Cuando termines la lista, vuelve a llamar a `mcp__kuador__pending_work` una vez: los issues entregados pueden haber llegado a otra etapa de tus agentes. Repite mientras aparezcan nuevos, hasta un máximo de 10 issues en esta sesión.
5. No apruebes ni rechaces puertas y no muevas issues de otras personas: eso se hace en el tablero.
6. Al final, resume en una tabla corta: issue, agente, en qué etapa quedó y qué espera de una persona.
