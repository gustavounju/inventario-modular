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

## Archivo

```text
scripts/windows/inventario-modular.ps1
```

## Datos capturados

- nombre de PC;
- ultimo usuario local logueado;
- fuero opcional por parametro o variable de entorno;
- primera IPv4 util;
- sistema operativo;
- procesador;
- RAM total en MB;
- impresora predeterminada o primera impresora fisica detectada;
- estado activo.

## Uso local

Con la app levantada en la misma maquina:

```powershell
.\scripts\windows\inventario-modular.ps1
```

Para apuntar a la IP LAN de la maquina de Gustavo:

```powershell
.\scripts\windows\inventario-modular.ps1 `
  -ServerUrl "http://192.168.1.8:8081/api/v1/equipos/inventario"
```

Para probar sin enviar:

```powershell
.\scripts\windows\inventario-modular.ps1 -DryRun
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

## Respaldo local

Si el servidor no responde, el script guarda el JSON en:

```text
C:\ProgramData\InventarioModular
```

Ese archivo permite reenviar o analizar el reporte cuando vuelva la conectividad.

## Pendiente

- sumar discos, seriales, motherboard, monitores, teclado, mouse y salud SMART;
- definir cola de reenvio automatico de reportes pendientes;
- rotar el token de laboratorio antes de usarlo fuera de casa;
- separar permisos de maquina por sede o segmento de red si se instala masivamente.
