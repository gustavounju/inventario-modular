# Modulo Equipos

## Objetivo

`EQUIPOS` es el primer modulo funcional de Inventario Modular. Representa el inventario
tecnico de PCs y dispositivos principales que luego alimentaran dashboard, tareas,
reportes, actas, stock asignado y mapas.

Esta primera version no intenta copiar todo el inventario viejo. Define una base limpia y
API-first para empezar a recibir y consultar equipos.

## Datos iniciales

La migracion Flyway esta en:

```text
src/main/resources/db/migration/V3__equipos_inicial.sql
```

Crea la tabla `equipos` con estos campos principales:

- `nombre`: identificador tecnico de la PC o equipo, unico y normalizado en mayusculas.
- `ultimo_usuario`: ultimo usuario informado por el inventario.
- `fuero`: area o fuero asociado.
- `ip`: direccion IPv4 o IPv6 informada.
- `sistema_operativo`: sistema reportado por la PC.
- `procesador`: procesador reportado.
- `ram_mb`: memoria RAM en megabytes.
- `impresora`: impresora asociada o detectada.
- `monitoreo`: estado inicial `SIN_REPORTE` o `REPORTADO`.
- `activo`: permite ocultar o desactivar equipos sin borrar historial.
- `ultimo_reporte_en`: fecha/hora del ultimo reporte recibido.

## API

Los endpoints quedan bajo `/api/v1/equipos`.

### Listar equipos

```http
GET /api/v1/equipos?q=mesa&page=0&pageSize=25
```

Requiere permiso:

```text
EQUIPOS:VER
```

La busqueda `q` filtra por nombre del equipo, ultimo usuario o fuero.

### Ver detalle

```http
GET /api/v1/equipos/{id}
```

Requiere permiso:

```text
EQUIPOS:VER
```

Devuelve los campos de listado mas `procesador`, `ramMb`, `impresora` y
`ultimoReporteEn`.

### Recibir inventario

```http
POST /api/v1/equipos/inventario
```

Requiere permiso:

```text
EQUIPOS:EDITAR
```

Payload:

```json
{
  "nombre": "pc-nueva-003",
  "ultimoUsuario": "jlopez",
  "fuero": "Informatica",
  "ip": "10.15.2.12",
  "sistemaOperativo": "Windows 11 Pro",
  "procesador": "AMD Ryzen 5",
  "ramMb": 16384,
  "ramDetalles": "2x8GB DDR4 3200MHz",
  "ramSeriales": "RAM-001 | RAM-002",
  "discosModelos": "WD Blue SSD",
  "discosSeriales": "DISK-001",
  "motherboardModelo": "ASUS PRIME",
  "motherboardSerial": "MB-123",
  "monitores": "Samsung 24 SN-456",
  "teclado": "Logitech K120",
  "mouse": "Logitech M90",
  "impresora": "Ricoh Mesa",
  "activo": true
}
```

Script Windows inicial servido por la app:

```text
src/main/resources/static/scripts/windows/inventario-modular.ps1
```

Desde `/login` se puede copiar un comando que toma automaticamente la IP y puerto del
servidor desde donde se abrio la pantalla.

Uso manual local:

```powershell
$u='http://192.168.1.8:8081'; $p="$env:TEMP\inventario-modular.ps1"; $h=(iwr "$u/scripts/windows/inventario-modular.ps1.sha256" -UseBasicParsing).Content.Trim(); iwr "$u/scripts/windows/inventario-modular.ps1" -UseBasicParsing -OutFile $p; $sha=[System.Security.Cryptography.SHA256]::Create(); $fs=[System.IO.File]::OpenRead($p); try{$a=([BitConverter]::ToString($sha.ComputeHash($fs))).Replace('-','').ToLowerInvariant()}finally{$fs.Close()}; if($a -ne $h){throw "SHA-256 invalido. Script descargado no coincide con el publicado por el servidor."}; powershell -NoProfile -File $p -ServerUrl "$u/api/v1/equipos/inventario"
```

Guia operativa:

- [Script de inventario Windows](script-inventario-windows.md)
- [Actualizacion de produccion](actualizacion-produccion-inventario-modular.md)

Comportamiento:

- si `nombre` no existe, crea el equipo;
- si `nombre` ya existe, actualiza los datos reportados;
- normaliza `nombre` a mayusculas;
- marca `monitoreo` como `REPORTADO`;
- registra `ultimo_reporte_en`.

## Pantalla

La pantalla inicial esta en:

```text
/admin/equipos
```

Se muestra desde `/admin` solo cuando el usuario tiene permiso `EQUIPOS:VER`.

Incluye buscador por PC, usuario o fuero, listado tabular, estado de monitoreo y datos
basicos: equipo, ultimo usuario, fuero, IP y sistema operativo.

## Seguridad

La autorizacion se resuelve con `AuthorizationService`:

- `GET /api/v1/equipos`: `EQUIPOS:VER`;
- `GET /api/v1/equipos/{id}`: `EQUIPOS:VER`;
- `POST /api/v1/equipos/inventario`: `EQUIPOS:EDITAR`;
- `/admin/equipos`: `EQUIPOS:VER`.

Un usuario autenticado pero sin permisos recibe `403 Forbidden`.

## Pruebas

Tests relacionados:

```text
src/test/java/ar/gov/justiciajujuy/sanpedro/inventario/web/EquipoControllerTests.java
src/test/java/ar/gov/justiciajujuy/sanpedro/inventario/web/EquipoPageControllerTests.java
src/test/java/ar/gov/justiciajujuy/sanpedro/inventario/web/AdminControllerTests.java
```

Comando:

```powershell
mvn "-Dtest=EquipoControllerTests,EquipoPageControllerTests,AdminControllerTests" test
```

## Pendiente

- Agregar detalle visual por equipo.
- Agregar edicion manual controlada.
- Agregar importacion inicial desde el inventario viejo.
- Incorporar relacion futura con stock, componentes, ubicaciones y actas.
- Definir si se implementa reenvio automatico de reportes pendientes cuando una PC no
  puede conectarse al servidor.
