# Runbook Para Manana: Ubuntu Por PuTTY

Guia para continuar en el trabajo desde el servidor Ubuntu, usando PuTTY/SSH.

## Alcance

Este runbook es para preparar una instalacion de laboratorio/staging del nuevo
Inventario Modular Java en el servidor Ubuntu o en una ubicacion separada autorizada.

No reemplaza el inventario actual. No toca la base `inventario_prod`. No reinicia servicios
existentes.

## Reglas de seguridad

Antes de ejecutar comandos en PuTTY:

1. Confirmar que se esta en el servidor correcto.
2. Confirmar que no se esta dentro de `/opt/inventario` del sistema actual.
3. Confirmar que no se va a tocar `inventario_prod`.
4. Confirmar que la carpeta nueva sera separada, por ejemplo `/opt/inventario-modular`.
5. Si un comando requiere `sudo`, leerlo completo antes de ejecutarlo.

## Objetivo de manana

Dejar el proyecto clonado desde GitLab en una carpeta nueva y comprobar:

- Que el servidor Ubuntu tiene Java 21 o puede instalarlo.
- Que puede clonar el repositorio GitLab.
- Que puede compilar el proyecto.
- Que puede generar el `.jar`.
- Que se entiende donde se veran los pipelines de CI/CD en GitLab.

No se busca dejarlo en produccion.

## Paso 1: Conectar por PuTTY

Conectarse al servidor Ubuntu usando la sesion habitual de PuTTY.

Al entrar, verificar usuario y host:

```bash
whoami
hostname
pwd
```

## Paso 2: Elegir carpeta separada

La carpeta sugerida para laboratorio es:

```text
/opt/inventario-modular
```

Antes de crearla, verificar que no exista:

```bash
ls -ld /opt/inventario-modular
```

Si no existe, crearla:

```bash
sudo mkdir -p /opt/inventario-modular
sudo chown "$USER":"$USER" /opt/inventario-modular
```

## Paso 3: Clonar desde GitLab

Entrar a `/opt`:

```bash
cd /opt
```

Clonar la rama `primeros-pasos` desde GitLab:

```bash
git clone --branch primeros-pasos https://gitlab.com/gustavoeliasm/inventario-modular.git inventario-modular
```

Entrar al proyecto:

```bash
cd /opt/inventario-modular
```

Verificar rama y ultimo commit:

```bash
git status
git log --oneline -5
```

El ultimo commit esperado debe coincidir con el ultimo subido a GitLab.

## Paso 4: Verificar Java

Verificar Java:

```bash
java -version
```

Debe ser Java 21.

Si aparece Java 8, Java 11, o el comando no existe, no continuar con despliegue. Primero
hay que instalar o configurar JDK 21 en Ubuntu.

Comando orientativo para Ubuntu/Debian, solo si esta autorizado:

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk
```

Verificar de nuevo:

```bash
java -version
```

## Paso 5: Compilar con Maven Wrapper

En Ubuntu se puede usar el Maven Wrapper del proyecto. No hace falta instalar Maven global
si el wrapper descarga Maven correctamente.

Desde:

```bash
cd /opt/inventario-modular
```

Ejecutar tests:

```bash
sh ./mvnw --batch-mode test
```

Construir el `.jar`:

```bash
sh ./mvnw --batch-mode -DskipTests package
```

Salida esperada:

```text
BUILD SUCCESS
```

Artefacto esperado:

```text
target/inventario-modular-0.0.1-SNAPSHOT.jar
```

## Paso 6: Base de datos

La base definida para el nuevo sistema es:

```text
inventario_modular
```

Importante:

- No usar `inventario_prod`.
- No modificar tablas del inventario actual.
- No correr migraciones contra una base remota sin autorizacion.

Si se autoriza crear una base de laboratorio en MySQL del servidor:

```sql
CREATE DATABASE inventario_modular
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Tambien debe definirse un usuario propio para la app, con permisos limitados solo sobre
esa base. No usar root para la aplicacion.

Ejemplo orientativo para DBA o entorno local autorizado:

```sql
CREATE USER 'inventario_modular_app'@'localhost' IDENTIFIED BY 'CAMBIAR_EN_EL_SERVIDOR';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON inventario_modular.* TO 'inventario_modular_app'@'localhost';
FLUSH PRIVILEGES;
```

No commitear esa clave. No pegarla en documentacion.

## Paso 7: Configuracion local del servidor

La configuracion real debe quedar fuera de git.

Variables esperadas:

```bash
export INVENTARIO_DB_URL="jdbc:mysql://127.0.0.1:3306/inventario_modular"
export INVENTARIO_DB_USER="inventario_modular_app"
export INVENTARIO_DB_PASSWORD="CAMBIAR_EN_EL_SERVIDOR"
export INVENTARIO_LDAP_URL="ldap://SERVIDOR_AD:389"
export INVENTARIO_LDAP_DOMAIN="DOMINIO"
export INVENTARIO_LDAP_BASE_DN="DC=ejemplo,DC=local"
```

Todavia falta confirmar los datos reales de Active Directory.

## Paso 8: Prueba manual sin servicio systemd

Solo para laboratorio, se puede probar el `.jar` sin instalar servicio:

```bash
java -jar target/inventario-modular-0.0.1-SNAPSHOT.jar
```

Si la app intenta conectar a MySQL y faltan variables, puede fallar. Eso es esperable hasta
crear la base y configurar credenciales locales.

No crear servicio systemd todavia. No tocar nginx todavia.

## Paso 9: Ver CI/CD en GitLab

Abrir GitLab:

```text
https://gitlab.com/gustavoeliasm/inventario-modular
```

Buscar:

```text
Build -> Pipelines
```

Verificar que el pipeline de la rama `primeros-pasos` tenga las etapas:

```text
validar
construir
```

La etapa `validar` debe correr tests. La etapa `construir` debe generar el artefacto
`.jar`.

## Paso 10: Sincronizar GitHub desde Windows

GitHub es copia de seguridad y practica de versionado. Desde Windows:

```powershell
cd "G:\unju2025\google gravity\inventario-modular"
git push github primeros-pasos
```

## Que queda para hacer despues

- Confirmar datos reales de Active Directory.
- Crear migraciones Flyway iniciales.
- Crear tablas de modulos, permisos, roles y usuarios internos.
- Implementar autenticacion AD.
- Implementar autorizacion local por modulos.
- Agregar tests de seguridad.
- Definir ambiente staging real.
- Recien despues evaluar systemd, nginx y despliegue productivo.

## Criterio de exito de manana

Se considera exitoso si:

- El repo se clona desde GitLab en una carpeta nueva.
- Java 21 queda disponible en Ubuntu.
- `sh ./mvnw --batch-mode test` termina con `BUILD SUCCESS`.
- `sh ./mvnw --batch-mode -DskipTests package` genera el `.jar`.
- Se puede ver el pipeline en GitLab.
- No se toca el inventario actual ni la base `inventario_prod`.
