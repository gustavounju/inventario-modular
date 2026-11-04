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

## Visor Kanban y Barra Electoral de Rendimiento Técnico

En la vista administrativa y modo TV `/admin/tareas/visor` se incorporó un componente visual inspirado en los tableros de resultados electorales para monitorear el desempeño del equipo técnico y el estado de resolución en tiempo real:

- **Marcador Superior (Estilo Resultados):**
  - **Lado Izquierdo:** Técnico líder con mayor cantidad de tareas resueltas en el período seleccionado y su color distintivo.
  - **Centro:** Total de tareas resueltas y subtítulo con la cantidad de tareas por asignar / pendientes.
  - **Lado Derecho:** Segundo técnico en volumen de resolución o equipo técnico general.
- **Barra Segmentada Interactiva (100% de la carga de trabajo):**
  - Cada técnico resolutor cuenta con un segmento proporcional a su aporte sobre el total de tareas, identificado con una paleta cromática armónica y accesible.
  - El segmento gris/central (`#94a3b8`) representa las tareas pendientes y en proceso (backlog por asignar).
  - **Tooltip contextual al desplazarse:** Al pasar el cursor por cualquier segmento o chip de leyenda, se muestra una tarjeta flotante con el nombre del técnico, cantidad de tareas resueltas y porcentaje respecto al total.
- **Filtros Temporales Rápidos:**
  - Selector con botones con estados accesibles (`aria-pressed`):
    - `Hoy`: Productividad del día actual.
    - `Esta Semana`: Desde el lunes de la semana en curso.
    - `Este Mes`: Acumulado del mes calendario corriente.
    - `Histórico`: Total acumulado registrado en el sistema.
- **Apartado Especial: Telemetría de Mesa de Entrada / Telefonista (`🎧`):**
  - Identifica el rol y nombre del operador/a que recepciona y crea los incidentes (ej. *Marta López*).
  - Expone métricas de tareas ingresadas en el período seleccionado, tareas ingresadas hoy y total histórico.
  - **Aislamiento métrico:** No se computa dentro de los segmentos de resolución técnica para evitar duplicar el total de incidencias del sistema.
- **Endpoint REST:**
  - `GET /api/v1/tareas/estadisticas-rendimiento?rango={HOY|SEMANA|MES|HISTORICO}`
  - Devuelve JSON estructurado con totales, porcentajes, desglose por técnico, líder, segundo y métricas de telefonía.

## Pendientes naturales

- Adjuntar actas, fotos o comprobantes.
- Vistas por responsable y por fuero.
- Exportacion CSV.
- Vincular tareas con actas/reportes cuando esos modulos existan.
