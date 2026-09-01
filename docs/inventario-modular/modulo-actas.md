# Modulo Actas

`ACTAS` registra constancias administrativas asociadas al inventario: entregas,
recepciones, devoluciones, traslados, bajas u otras actuaciones internas.

## Alcance implementado

- Migracion Flyway `V10__actas_ubicaciones.sql`.
- Tabla `actas` con numero unico, tipo, equipo opcional, fecha, destinatario,
  responsables, detalle, estado y observaciones.
- API protegida por permisos:

```text
GET  /api/v1/actas
POST /api/v1/actas
PUT  /api/v1/actas/{id}
```

- Pantalla administrativa:

```text
/admin/actas
```

- Filtros por texto, tipo y estado.
- Alta y edicion desde pantalla si el usuario tiene `ACTAS:EDITAR`.
- Asociacion opcional a un equipo existente.
- Auditoria al crear y actualizar.
- CSV desde Reportes:

```text
GET /api/v1/reportes/actas.csv
```

## Estados

```text
BORRADOR
EMITIDA
ANULADA
```

## Tipos

```text
ENTREGA
RECEPCION
DEVOLUCION
TRASLADO
BAJA
OTRA
```

## Pendientes

- Generar PDF/impresion formal de cada acta.
- Numeracion automatica por anio o dependencia.
- Adjuntos y firmas.
- Vincular actas con bienes patrimoniales y componentes, no solo con equipos.
