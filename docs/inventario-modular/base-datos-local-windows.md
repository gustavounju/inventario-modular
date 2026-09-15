# Base De Datos Local Windows

Guia para preparar Inventario Modular en una PC Windows con MySQL local como fallback.
El contrato operativo actual es probar siempre MySQL remoto primero y usar MySQL local
solo si el remoto no responde o no autentica.

## Alcance

Estos pasos preparan `127.0.0.1:3306` como contingencia. El arranque normal intenta primero
`MYSQL_INTERNO_IP:3306` y recien despues usa esta base local.

Produccion y trabajo siguen usando MySQL remoto mediante variables del servicio Ubuntu o del
script local. No escribir credenciales reales en documentos, commits ni chat.

## Datos locales

```text
Host: 127.0.0.1
Puerto: 3306
Base: inventario_modular
Usuario de aplicacion: inventario_local
```

## Paso 1: Verificar MySQL local

Desde PowerShell:

```powershell
Test-NetConnection -ComputerName 127.0.0.1 -Port 3306
```

La salida correcta debe incluir:

```text
TcpTestSucceeded : True
```

En esta PC se detecto el servicio `MySQL84` en estado `Running` y el cliente:

```text
C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe
```

## Paso 2: Crear o reparar la base local

Ejecutar el script del repo:

```powershell
.\scripts\setup-local-mysql.ps1
```

El script pide la clave de `root` de MySQL local en la consola. No escribir claves en
documentos, commits ni chat.

El script crea o actualiza:

```text
Base: inventario_modular
Usuario: inventario_local
```

Si aparece:

```text
Access denied for user 'inventario_local'@'localhost'
```

significa que MySQL local responde, pero el usuario `inventario_local` no existe, no tiene
permisos sobre `inventario_modular` o la clave ingresada no coincide. Ejecutar
`setup-local-mysql.ps1` corrige ese estado.

## Paso 3: Arrancar local con MySQL y Active Directory

Con la base local preparada:

```powershell
.\scripts\start-local-ad.ps1
```

El script prueba primero MySQL remoto. Si responde, pide:

```text
Usuario MySQL REMOTO [inventario_modular_app]
Clave MySQL REMOTO
Usuario lector AD
Clave AD
```

Si MySQL remoto no responde o no autentica, recien pide:

```text
Usuario MySQL LOCAL [inventario_local]
Clave MySQL LOCAL
Usuario lector AD
Clave AD
```

Si Active Directory no responde, por ejemplo en casa sin VPN/red interna, el script no se
corta: arranca con `INVENTARIO_LDAP_ENABLED=false` y usa los usuarios locales de la base.

Para AD local en el trabajo usa:

```text
LDAP URL: ldap://10.15.0.41:389
Base de login: DC=podjudsp,DC=local
Base de busqueda de usuarios: OU=USUARIOS,OU=PODJUDSP
Dominio: podjudsp.local
```

La base de login es la raiz del dominio para que Spring Security pueda encontrar el
usuario autenticado. La busqueda/autocompletado de solicitantes queda limitada a la OU
de usuarios.

Formatos aceptados para AD:

```text
PODJUDSP\usuario
usuario@podjudsp.local
```

Al iniciar, imprime:

```text
http://localhost:8081
http://IP-DE-LA-PC:8081/movil/login
```

La segunda URL es la que debe probarse desde el celular.

## Configuracion aplicada

El perfil `local` usa MySQL remoto como primario:

```properties
inventario.datasource.primary.url=jdbc:mysql://MYSQL_INTERNO_IP:3306/inventario_modular
inventario.datasource.primary.username=inventario_modular_app
inventario.datasource.fallback.url=jdbc:mysql://127.0.0.1:3306/inventario_modular
inventario.datasource.fallback.username=inventario_local
```

La unidad systemd o el archivo de entorno del servidor debe definir:

```text
SPRING_PROFILES_ACTIVE=local
INVENTARIO_DB_PRIMARY_URL=jdbc:mysql://MYSQL_INTERNO_IP:3306/inventario_modular
INVENTARIO_DB_PRIMARY_USER=inventario_modular_app
INVENTARIO_DB_PRIMARY_PASSWORD=...
```

Esa clave vive solo en `/etc/inventario-modular/inventario-modular.env` del servidor
Ubuntu y no se versiona.

## Modo H2

El perfil `casa` con H2 queda solo como laboratorio aislado cuando no hay MySQL. No debe
usarse para validar el flujo de trabajo ni para pruebas con celulares en el edificio.

## Comandos prohibidos en el flujo normal

No usar estos comandos durante la instalacion local normal:

```sql
DROP DATABASE inventario_modular;
DROP USER 'inventario_local'@'localhost';
```

Si alguna vez hiciera falta borrar una base, se decide aparte y con backup.
