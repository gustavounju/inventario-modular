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
- API `PUT /api/v1/tareas-tecnicas/{id}`.
- API `PATCH /api/v1/tareas-tecnicas/{id}/estado`.
- API `GET /api/v1/tareas-tecnicas/{id}/comentarios`.
- API `POST /api/v1/tareas-tecnicas/{id}/comentarios`.
- Pantalla administrativa `/admin/tareas`.
- Filtros por estado, equipo y texto operativo.
- Alta de tareas desde la pantalla.
- Edicion de titulo, descripcion, equipo, prioridad y responsable desde la pantalla.
- Cambio de estado desde la pantalla.
- Comentarios/historial operativo por tarea desde API y pantalla.
- Auditoria al crear, editar, comentar y cambiar estado.
- Web movil `/movil/tareas` para tecnicos, administradores desde celular y mesa telefonica.
- Rol `TELEFONISTA` para recepcionar llamados y publicar tareas.
- Responsable opcional al crear una tarea, con aviso general o dirigido segun corresponda.
- Trazabilidad de componentes usados desde stock y aviso visual cuando luego vuelven a stock.

## Modelo inicial

Cada tarea registra:

- equipo asociado opcional;
- titulo;
- descripcion;
- estado;
- prioridad;
- responsable;
- solicitante y fuero/oficina;
- observaciones de cierre;
- fecha de cierre cuando corresponde.
- comentarios de seguimiento con autor y fecha.

## Reglas operativas vigentes

- `ADMIN` conserva administracion completa en PC. Desde celular usa la misma experiencia que un tecnico.
- `TECNICO` toma, comenta, finaliza, cancela y opera tareas asignadas o creadas por el.
- `TELEFONISTA` crea tareas nuevas, comenta siempre sus tareas, edita una tarea propia antes
  de que sea tomada y puede borrarla si todavia no tiene responsable.
- Una tarea sin responsable dispara aviso general a celulares de tecnicos y administradores.
- Una tarea con responsable elegido al crear, o asignado despues, dispara aviso dirigido solo
  a ese tecnico.
- El desplegable/autocompletado de responsable usa tecnicos locales y coincidencias de Active
  Directory cuando LDAP esta disponible.

## Stock asociado a tareas

Cuando una tarea instala o reserva componentes desde stock, el historial queda en
`tareas_stock_uso`. Si un componente se desvincula luego desde la ficha del equipo y vuelve a
`DISPONIBLE`, la tarea conserva el registro original pero lo muestra como **Devuelto a stock**,
con aviso visual y, para desvinculaciones nuevas, usuario, fecha, equipo y motivo.

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

- Adjuntar actas, fotos o comprobantes.
- Vistas por responsable y por fuero.
- Exportacion CSV.
- Vincular tareas con actas/reportes cuando esos modulos existan.
