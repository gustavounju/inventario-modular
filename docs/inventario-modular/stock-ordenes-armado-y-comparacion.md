# Stock, Ordenes De Armado Y Comparacion

## Objetivo

Este documento deja la primera version funcional del circuito confirmado para el gemelo
digital:

```text
COMPONENTES
-> RELEVAMIENTO_INICIAL
-> STOCK
-> ORDENES_ARMADO
-> COMPARACION DEL GEMELO DIGITAL
```

La idea practica es que el sistema pueda trabajar con dos caminos:

- PC vieja: el script detecta hardware y eso se usa como base inicial del equipo.
- PC nueva: se cargan componentes en stock, se arma una orden y despues se compara contra
  lo que detecta el script.

## Migracion

La migracion principal esta en:

```text
src/main/resources/db/migration/V6__stock_ordenes_armado_comparacion.sql
```

Crea:

- `stock_componentes`: componentes sueltos disponibles, reservados, asignados o dados de
  baja.
- `ordenes_armado`: orden de armado o mejora vinculada a un equipo.
- `orden_armado_componentes`: relacion entre una orden, el componente esperado del
  gemelo digital y, si corresponde, el componente de stock reservado.

Tambien agrega el modulo:

```text
ORDENES_ARMADO
```

El modulo `STOCK` ya existia en el seed principal de seguridad modular.

## Stock

Pantalla web:

```text
/admin/stock
```

Desde ahi se puede:

- Ver componentes cargados.
- Cargar componentes sueltos nuevos.
- Identificar tipo, estado, marca, modelo, serial, capacidad, ubicacion y observaciones.
- Editar componentes ya cargados para corregir estado, serial, marca, modelo, capacidad,
  ubicacion, observaciones o activo.

Endpoint para listar componentes de stock:

```http
GET /api/v1/stock/componentes
```

Requiere:

```text
STOCK:VER
```

Endpoint para cargar componente suelto:

```http
POST /api/v1/stock/componentes
```

Requiere:

```text
STOCK:EDITAR
```

Payload ejemplo:

```json
{
  "tipo": "DISCO",
  "estado": "DISPONIBLE",
  "descripcion": "SSD nuevo para armado",
  "marca": "Kingston",
  "modelo": "SA400",
  "serial": "DISK-001",
  "capacidad": "480GB",
  "ubicacion": "Deposito Informatica",
  "observaciones": "Alta inicial de stock",
  "activo": true
}
```

Estados iniciales de stock:

```text
DISPONIBLE -> puede reservarse para una orden.
RESERVADO  -> fue tomado por una orden de armado.
ASIGNADO   -> tuvo salida real confirmada desde una orden de armado.
BAJA       -> no debe usarse.
```

## Ordenes de armado

Pantalla web:

```text
/admin/ordenes-armado
```

Desde ahi se puede:

- Seleccionar un equipo.
- Ver sus ordenes de armado.
- Crear una orden nueva.
- Editar estado, descripcion y observaciones de ordenes existentes.
- Agregar componentes esperados seleccionando la orden exacta del equipo.
- Reservar un componente disponible de stock para una orden.
- Ir al gemelo digital del equipo para ver la comparacion.

Endpoint para listar ordenes de un equipo:

```http
GET /api/v1/equipos/{equipoId}/ordenes-armado
```

Endpoint para crear orden:

```http
POST /api/v1/equipos/{equipoId}/ordenes-armado
```

Requieren permiso:

```text
ORDENES_ARMADO:VER
ORDENES_ARMADO:EDITAR
```

Payload ejemplo:

```json
{
  "estado": "EN_ARMADO",
  "descripcion": "Armado de PC-INF-001",
  "observaciones": "Orden inicial para comparar con script"
}
```

Estados iniciales de orden:

```text
BORRADOR
EN_ARMADO
ESPERANDO_REPORTE
COMPARADA
CERRADA
CANCELADA
```

Agregar componente esperado a una orden:

```http
POST /api/v1/ordenes-armado/{ordenId}/componentes
```

Payload ejemplo:

```json
{
  "stockComponenteId": 2,
  "tipo": "DISCO",
  "descripcion": "Disco esperado desde stock",
  "marca": "Kingston",
  "modelo": "SA400",
  "serial": "DISK-001",
  "capacidad": "480GB",
  "ubicacion": "SATA 1",
  "observaciones": "Debe coincidir con el reporte"
}
```

Cuando se agrega el componente esperado:

- Se crea un componente del equipo con `origen = ORDEN_ARMADO`.
- Se marca con `estado_comparacion = ESPERADO`.
- Si se indico `stockComponenteId`, el componente de stock pasa a `RESERVADO`.

Confirmar salida real desde stock:

```http
POST /api/v1/ordenes-armado/componentes/{ordenComponenteId}/confirmar-salida-stock
```

Cuando se confirma la salida:

- El componente de stock asociado pasa de `RESERVADO` a `ASIGNADO`.
- El componente esperado del gemelo digital conserva sus datos y pasa a
  `origen = STOCK`.
- La comparacion lo sigue tomando como componente esperado.
- Si el componente de orden no tenia stock asociado, la operacion responde conflicto.

## Comparacion del gemelo digital

Endpoint:

```http
GET /api/v1/equipos/{equipoId}/gemelo-digital/comparacion
```

Requiere:

```text
COMPONENTES:VER
```

Regla refinada:

- Esperado: componentes `ORDEN_ARMADO` o `STOCK`, o estado `ESPERADO`.
- Detectado: componentes `SCRIPT` o `RELEVAMIENTO_INICIAL`.
- Coincide: mismo tipo y mismo serial normalizado; si ambos lados no tienen serial, se
  compara modelo, descripcion o capacidad con modelo/descripcion compatible.
- Revisar: mismo tipo con datos parecidos, pero falta confirmar serial, modelo o
  capacidad.
- Falta: estaba esperado pero no aparece detectado.
- Sobra: aparece detectado pero no estaba esperado.
- La comparacion es uno-a-uno: un componente detectado no puede cerrar dos componentes
  esperados.

La pantalla `/admin/equipos/{id}` muestra una seccion:

```text
Comparacion del gemelo digital
```

Alli se ve el cruce entre esperado y detectado sin tener que entrar a un endpoint manual.

## Dashboard de diferencias

El resumen transversal esta disponible en:

```text
/admin/dashboard-diferencias
```

Muestra:

- Conteo de componentes `FALTA`.
- Conteo de componentes `SOBRA`.
- Conteo de componentes `REVISAR`.
- Conteo de componentes `COINCIDE`.
- Equipos con diferencias pendientes.
- Acceso directo al detalle de cada equipo.

API:

```http
GET /api/v1/gemelo-digital/dashboard-diferencias
```

Requiere:

```text
COMPONENTES:VER
```

## Relevamiento inicial desde script

Para una PC vieja, el tecnico puede entrar al detalle del equipo y usar la accion:

```text
Consolidar lectura como relevamiento inicial
```

Esto toma la ultima lectura activa de origen `SCRIPT`, reemplaza el relevamiento inicial
anterior de ese equipo y guarda una base estable con origen `RELEVAMIENTO_INICIAL`.
La comparacion sigue tomando esa base como dato detectado.

## Auditoria transversal

La primera version de auditoria registra cambios relevantes del circuito:

- Componentes: creacion, actualizacion, consolidacion de relevamiento inicial y registro
  desde script.
- Stock: creacion, actualizacion, reserva y asignacion.
- Ordenes de armado: creacion, actualizacion, agregado de componente esperado y
  confirmacion de salida real desde stock.

Consulta visual:

```text
/admin/auditoria
```

API:

```http
GET /api/v1/auditoria/eventos
```

Requiere:

```text
AUDITORIA:VER
```

## Comandos usados

Pruebas enfocadas:

```powershell
.\mvnw.cmd "-Dtest=EquipoControllerTests,ComponenteControllerTests,EquipoPageControllerTests,StockOrdenArmadoControllerTests,CurrentUserControllerTests" test
.\mvnw.cmd "-Dtest=AdminControllerTests,StockOrdenArmadoPageControllerTests,StockOrdenArmadoControllerTests,EquipoPageControllerTests" test
.\mvnw.cmd "-Dtest=ComponenteControllerTests,EquipoPageControllerTests" test
.\mvnw.cmd "-Dtest=StockOrdenArmadoControllerTests,StockOrdenArmadoPageControllerTests" test
.\mvnw.cmd "-Dtest=ComponenteControllerTests,StockOrdenArmadoControllerTests,EquipoPageControllerTests" test
.\mvnw.cmd "-Dtest=AuditoriaControllerTests,AdminControllerTests,ComponenteControllerTests,StockOrdenArmadoControllerTests,StockOrdenArmadoPageControllerTests" test
.\mvnw.cmd "-Dtest=DashboardDiferenciasControllerTests,AdminControllerTests,ComponenteControllerTests" test
```

Suite completa:

```powershell
.\mvnw.cmd --batch-mode test
```
