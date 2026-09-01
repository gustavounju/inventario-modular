# Modulo Tareas Tecnicas

## Objetivo

Registrar trabajos operativos del equipo de Informatica vinculados al inventario modular:
mantenimiento, reparaciones, revisiones preventivas y seguimientos que pueden estar
asociados a un equipo concreto o quedar como tarea general.

## Alcance implementado

- Migracion Flyway `V8__tareas_tecnicas.sql`.
- Tabla `tareas_tecnicas`.
- Modulo de permisos `TAREAS`.
- API `GET /api/v1/tareas-tecnicas`.
- API `POST /api/v1/tareas-tecnicas`.
- API `PATCH /api/v1/tareas-tecnicas/{id}/estado`.
- Pantalla administrativa `/admin/tareas`.
- Filtros por estado, equipo y responsable.
- Alta de tareas desde la pantalla.
- Cambio de estado desde la pantalla.
- Auditoria al crear y cambiar estado.

## Modelo inicial

Cada tarea registra:

- equipo asociado opcional;
- titulo;
- descripcion;
- estado;
- prioridad;
- responsable;
- observaciones de cierre;
- fecha de cierre cuando corresponde.

Estados iniciales:

```text
PENDIENTE
EN_PROCESO
CERRADA
CANCELADA
```

Prioridades iniciales:

```text
BAJA
MEDIA
ALTA
URGENTE
```

## Pendientes naturales

- Editar titulo, descripcion, prioridad y responsable de una tarea existente.
- Historial de cambios por tarea en la pantalla.
- Comentarios o novedades por tarea.
- Adjuntar actas, fotos o comprobantes.
- Vistas por responsable y por fuero.
- Exportacion CSV.
- Vincular tareas con actas/reportes cuando esos modulos existan.
