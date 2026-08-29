# Base De Datos Local Windows

Guia para crear la base MySQL local de Inventario Modular en una maquina Windows de casa.

## Alcance

Estos pasos son solo para desarrollo local. No se conectan al servidor MySQL del trabajo
`10.15.0.62` y no tocan ninguna base de produccion.

## Datos locales

```text
Host: 127.0.0.1
Puerto: 3306
Base: inventario_modular
Usuario de aplicacion: inventario_local
```

## Paso 1: Verificar que MySQL responde

Desde PowerShell:

```powershell
Test-NetConnection -ComputerName 127.0.0.1 -Port 3306
```

La salida correcta debe incluir:

```text
TcpTestSucceeded : True
```

Si da `False`, MySQL no esta iniciado, no esta instalado o escucha en otro puerto.

## Paso 2: Verificar cliente mysql

```powershell
mysql --version
```

Si PowerShell responde que `mysql` no se reconoce, hay que usar la ruta completa del
cliente MySQL o agregar su carpeta `bin` al `Path`.

Rutas habituales:

```text
C:\Program Files\MySQL\MySQL Server 8.0\bin
C:\Program Files\MariaDB 11.4\bin
```

Ejemplo con ruta completa:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p
```

## Paso 3: Entrar como administrador MySQL local

```powershell
mysql -u root -p
```

MySQL pedira la clave del usuario `root` local. Esa clave no debe guardarse en git ni en
documentacion.

## Paso 4: Crear base y usuario local

Dentro de MySQL:

```sql
CREATE DATABASE IF NOT EXISTS inventario_modular
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'inventario_local'@'localhost'
  IDENTIFIED BY 'Cambiar_Clave_Local_123!';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON inventario_modular.* TO 'inventario_local'@'localhost';

FLUSH PRIVILEGES;
```

Si el usuario ya existia y se quiere cambiar la clave local:

```sql
ALTER USER 'inventario_local'@'localhost'
  IDENTIFIED BY 'Cambiar_Clave_Local_123!';

FLUSH PRIVILEGES;
```

## Paso 5: Verificar permisos

```sql
SHOW DATABASES LIKE 'inventario_modular';
SHOW GRANTS FOR 'inventario_local'@'localhost';
```

Salir:

```sql
EXIT;
```

## Paso 6: Probar conexion con el usuario de la app

Desde PowerShell:

```powershell
mysql -u inventario_local -p -h 127.0.0.1 inventario_modular
```

Ingresar la clave local configurada. Si conecta, salir con:

```sql
EXIT;
```

## Paso 7: Variables para ejecutar Spring Boot

En la misma PowerShell donde se va a iniciar la app:

```powershell
$env:INVENTARIO_DB_URL = "jdbc:mysql://127.0.0.1:3306/inventario_modular"
$env:INVENTARIO_DB_USER = "inventario_local"
$env:INVENTARIO_DB_PASSWORD = "Cambiar_Clave_Local_123!"
```

Estas variables viven solo en esa terminal. No se commitean.

## Comandos prohibidos

No usar estos comandos durante la instalacion local normal:

```sql
DROP DATABASE inventario_modular;
DROP USER 'inventario_local'@'localhost';
```

Si alguna vez hiciera falta borrar una base, se decide aparte y con backup.
