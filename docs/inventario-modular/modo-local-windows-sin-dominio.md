# Modo Local Windows Sin Dominio

Guia para trabajar desde casa con Inventario Modular cuando no hay acceso al dominio real
del Poder Judicial.

## Objetivo

Permitir desarrollo local en Windows sin Active Directory, sin quitar ni debilitar el modo
real de produccion/trabajo.

En el trabajo:

```text
LDAP/Active Directory real -> usuario y clave de dominio
```

En casa:

```text
Login local simulado -> usuario configurado en application-local.properties o variables
```

## Regla de seguridad

El modo local solo funciona cuando:

```properties
inventario.local-auth.enabled=true
inventario.ldap.enabled=false
```

Si `inventario.ldap.enabled=true`, el proveedor local no se registra. Eso evita que una
instalacion configurada para Active Directory acepte por accidente el usuario local.

## Usuario local por defecto

El perfil `local` trae estos valores para estudiar:

```text
Usuario: admin.local
Clave: AdminLocal123!
Nombre visible: Administrador Local
Fuero: Desarrollo local
```

Son valores de desarrollo. En una maquina compartida conviene cambiarlos con variables de
entorno antes de iniciar la app.

## Variables recomendadas en PowerShell

Desde Windows:

```powershell
cd "G:\unju2025\google gravity\inventario-modular"

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:USERPROFILE\tools\apache-maven-3.9.16\bin;$env:Path"

$env:INVENTARIO_DB_URL = "jdbc:mysql://127.0.0.1:3306/inventario_modular"
$env:INVENTARIO_DB_USER = "inventario_local"
$env:INVENTARIO_DB_PASSWORD = "Cambiar_Clave_Local_123!"

$env:INVENTARIO_LDAP_ENABLED = "false"
$env:INVENTARIO_LOCAL_AUTH_ENABLED = "true"
$env:INVENTARIO_LOCAL_AUTH_USERNAME = "admin.local"
$env:INVENTARIO_LOCAL_AUTH_PASSWORD = "AdminLocal123!"
$env:INVENTARIO_LOCAL_AUTH_DISPLAY_NAME = "Administrador Local"
$env:INVENTARIO_LOCAL_AUTH_FUERO = "Desarrollo local"
```

## Ejecutar tests

```powershell
mvn test
```

Salida esperada:

```text
BUILD SUCCESS
```

## Iniciar la app

```powershell
mvn spring-boot:run
```

Abrir:

```text
http://localhost:8081/
```

Ingresar:

```text
Usuario: admin.local
Clave: AdminLocal123!
```

## Como esta implementado

- `LocalAuthenticationProperties` lee las propiedades `inventario.local-auth.*`.
- `LocalAuthenticationConfig` crea un usuario local en memoria.
- La clave local se codifica con BCrypt al arrancar.
- La pantalla admin recibe una identidad compatible con la usada por Active Directory.
- Los atributos mostrados indican que la sesion viene de `LOCAL_SIMULADO`.

## Que no hace

- No consulta Active Directory.
- No guarda claves de dominio.
- No crea usuarios en MySQL.
- No reemplaza el esquema definitivo de usuarios, roles, permisos y modulos.
- No debe usarse como mecanismo de ingreso productivo.

## Diferencia con el trabajo

En el trabajo, la app debe iniciar con:

```bash
INVENTARIO_LDAP_ENABLED="true"
INVENTARIO_LDAP_URL="ldap://10.15.0.41:389"
INVENTARIO_LDAP_DOMAIN="podjudsp.local"
INVENTARIO_LDAP_BASE_DN="OU=USUARIOS,OU=PODJUDSP,DC=podjudsp,DC=local"
```

Y la base debe apuntar a:

```bash
INVENTARIO_DB_URL="jdbc:mysql://10.15.0.62:3306/inventario_modular"
```

En casa, esos valores reales no se usan.
