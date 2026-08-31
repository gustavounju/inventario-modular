# Script de inventario Windows

## Objetivo

El script Windows captura datos basicos de una PC y los envia al endpoint API-first de
Inventario Modular:

```text
POST /api/v1/equipos/inventario
```

Es la primera version modular del flujo que antes resolvia `inventario.ps1` en el sistema
viejo. No reemplaza todavia toda la profundidad del script heredado: deja funcionando el
camino minimo confiable para cargar equipos reales y despues sumar componentes con mas
detalle.

## Archivo servido por la app

```text
src/main/resources/static/scripts/windows/inventario-modular.ps1
```

Cuando la app esta levantada, el script se puede descargar desde el mismo servidor:

```text
http://IP_DEL_SERVIDOR:8081/scripts/windows/inventario-modular.ps1
```

## Datos capturados

- nombre de PC;
- ultimo usuario local logueado;
- fuero opcional por parametro o variable de entorno;
- primera IPv4 util;
- sistema operativo;
- procesador;
- RAM total en MB;
- detalle y seriales de RAM;
- modelos y seriales de discos;
- modelo y serial de motherboard;
- monitores detectados;
- teclado y mouse;
- impresora predeterminada o primera impresora fisica detectada;
- estado activo.

No captura salud SMART en esta etapa.

## Copiar desde el login

La pantalla `/login` muestra un comando listo para copiar. Ese comando usa
`window.location.origin`, por eso toma automaticamente la IP y puerto desde donde se abrio
el login.

Ejemplo: si se abre el login en:

```text
http://192.168.1.8:8081/login
```

el comando copiado descarga el script desde:

```text
http://192.168.1.8:8081/scripts/windows/inventario-modular.ps1
```

y envia el reporte a:

```text
http://192.168.1.8:8081/api/v1/equipos/inventario
```

## Uso local

Con la app levantada en la misma maquina:

```powershell
iwr "http://localhost:8081/scripts/windows/inventario-modular.ps1" -UseBasicParsing -OutFile "$env:TEMP\inventario-modular.ps1"
powershell -ExecutionPolicy Bypass -File "$env:TEMP\inventario-modular.ps1"
```

Para apuntar a la IP LAN de la maquina de Gustavo:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:TEMP\inventario-modular.ps1" `
  -ServerUrl "http://192.168.1.8:8081/api/v1/equipos/inventario"
```

Para probar sin enviar:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:TEMP\inventario-modular.ps1" -DryRun
```

## Token

El endpoint acepta autenticacion de maquina por bearer token. En desarrollo local existe
un token de laboratorio, pero en trabajo debe definirse fuera de git:

```powershell
$env:INVENTARIO_REPORT_TOKEN = "TOKEN_REAL_DE_REPORTE"
```

La app debe arrancar con el mismo valor:

```powershell
$env:INVENTARIO_REPORT_TOKEN = "TOKEN_REAL_DE_REPORTE"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

En Ubuntu/systemd se debe cargar en el `EnvironmentFile` del servicio, no en el repositorio.

## Fuero

Si no se informa fuero, el backend intenta detectarlo por prefijo del nombre de PC y, si el
equipo ya existia, conserva el fuero anterior.

Para mandarlo explicitamente:

```powershell
.\scripts\windows\inventario-modular.ps1 `
  -Fuero "Dpto. Informatica San Pedro"
```

O por variable de entorno:

```powershell
$env:INVENTARIO_FUERO = "Dpto. Informatica San Pedro"
.\scripts\windows\inventario-modular.ps1
```

## Respaldo local, no reenvio automatico

Si el servidor no responde, el script guarda el JSON en:

```text
C:\ProgramData\InventarioModular
```

Ese archivo permite reenviar o analizar el reporte cuando vuelva la conectividad.

El "reenvio automatico" seria un paso posterior: el script podria revisar esa carpeta al
arrancar y mandar al servidor los reportes que quedaron pendientes. Todavia no se implementa
para mantener este primer flujo simple y visible.

## Pendiente

- definir cola de reenvio automatico de reportes pendientes;
- rotar el token de laboratorio antes de usarlo fuera de casa;
- separar permisos de maquina por sede o segmento de red si se instala masivamente.
