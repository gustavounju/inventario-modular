# Metodologia ITAM/CMDB de taller

## Objetivo

Inventario Modular adopta una metodologia ITAM/CMDB adaptada al trabajo real del taller
de informatica. No busca copiar una herramienta generica de mercado, sino ordenar lo que
ya hace el sistema: inventariar, comparar, reparar, documentar y decidir.

La pregunta central deja de ser solo:

```text
Que equipos tenemos?
```

Y pasa a ser:

```text
Que activos tenemos, que configuracion real tienen, que diferencia existe contra lo
esperado, que trabajo hace falta y que evidencia respalda cada cambio?
```

## Capas del modelo

### 1. Activo

El activo es el objeto institucional que se administra durante su ciclo de vida. En este
sistema, el activo principal es el equipo informatico.

Ejemplos:

- PC de usuario.
- PC de mesa de entrada.
- PC en taller.
- PC generica para tareas sin equipo identificado.

Implementacion actual:

- `Equipo`
- `equipos`
- `/admin/equipos`
- `/api/v1/equipos`

### 2. Configuracion esperada

Es lo que el taller o el administrador considera valido para el activo.

Puede venir de:

- carga manual;
- asignacion desde stock;
- orden de armado;
- correccion administrativa;
- decision de aceptar un componente detectado.

Implementacion actual:

- `Componente`
- `ComponenteService`
- componentes vinculados al equipo;
- datos de remito, orden de compra y proveedor cuando existan.

### 3. Configuracion detectada

Es lo que reporta el script de inventario o una fuente automatica.

No es automaticamente verdad administrativa. Es evidencia tecnica que debe compararse con
lo esperado.

Implementacion actual:

- script Windows de inventario;
- `POST /api/v1/equipos/inventario`;
- `EquipoService.registrarInventario`.

### 4. Gemelo digital

El gemelo digital es la vista consolidada del activo. Debe mostrar que se sabe del equipo,
que se esperaba, que se detecto y que requiere revision.

Implementacion actual:

- `GemeloDigitalService`;
- dashboard de diferencias;
- detalle del equipo.

### 5. Diferencia

Una diferencia es una desviacion entre configuracion esperada y configuracion detectada.

Tipos operativos:

- componente faltante;
- componente agregado;
- componente reemplazado;
- dato incompleto;
- serial/capacidad/modelo inconsistente;
- equipo sin reporte reciente;
- componente que requiere validacion humana.

Una diferencia importante no debe morir como alerta visual. Debe poder terminar en:

- tarea tecnica;
- correccion administrativa;
- aceptacion como nuevo estado valido;
- acta/evidencia;
- baja o reemplazo.

### 6. Stock

El stock representa capacidad de reparacion. No es solo una lista de piezas: es lo que el
taller puede usar para resolver tareas, armar equipos o corregir diferencias.

Estados relevantes:

- disponible;
- reservado;
- asignado;
- baja;
- pendiente de completar.

Implementacion actual:

- `StockComponente`;
- `StockService`;
- `/admin/stock`;
- `/movil/stock`;
- `tareas_stock_uso`.

### 7. Trabajo tecnico

El trabajo tecnico une decision con accion. Una tarea puede nacer por pedido humano, por
diferencia del gemelo, por falta de stock, por mantenimiento o por preparacion de un equipo.

Implementacion actual:

- `TareaTecnica`;
- `TareaTecnicaService`;
- APK LAN;
- `/movil/tareas`;
- visor publico `/admin/tareas/visor`.

### 8. Evidencia

La evidencia responde quien hizo que, cuando, sobre que activo y con que respaldo.

Ejemplos:

- acta;
- comentario de tarea;
- pieza de stock usada;
- auditoria;
- movimiento de equipo;
- cierre de tarea;
- reporte de inventario.

Implementacion actual:

- `Acta`;
- `AuditoriaService`;
- comentarios de tarea;
- `tareas_stock_uso`;
- movimientos.

## Ciclo de vida recomendado del activo

```text
Ingreso / alta
  -> configuracion inicial
  -> asignacion o taller
  -> reporte automatico
  -> comparacion con gemelo
  -> diferencia o conformidad
  -> tarea tecnica si hace falta
  -> uso de stock / intervencion
  -> evidencia
  -> cierre o nueva comparacion
  -> baja / retiro cuando corresponda
```

## Estados sugeridos para evolucion futura

Estos estados no deben agregarse sin migracion y pruebas, pero sirven como guia:

| Estado | Sentido operativo |
|---|---|
| `EN_TALLER` | Equipo en preparacion, reparacion o diagnostico |
| `EN_USO` | Equipo asignado y operativo |
| `EN_REVISION` | Requiere decision por diferencia o incidente |
| `RESERVADO` | Equipo preparado para destino definido |
| `BAJA_PENDIENTE` | Espera acta, autorizacion o retiro |
| `BAJA` | Fuera del circuito operativo |

## Reportes de decision

El panel general y reportes futuros deben priorizar:

- equipos con diferencias abiertas;
- equipos sin reporte reciente;
- tareas pendientes por prioridad;
- stock disponible para resolver tareas;
- stock critico o incompleto;
- piezas usadas por periodo;
- equipos intervenidos por tecnico;
- equipos en taller;
- activos candidatos a baja;
- cambios sin evidencia suficiente.

## Como debe influir en proximos cambios

Cuando se agregue una funcion nueva, ubicarla en una de estas capas:

- activo;
- configuracion esperada;
- configuracion detectada;
- gemelo;
- diferencia;
- stock;
- trabajo tecnico;
- evidencia;
- reporte de decision.

Si una funcion no entra en ninguna capa, probablemente sea una pantalla o dato suelto que
debe repensarse antes de programarse.

