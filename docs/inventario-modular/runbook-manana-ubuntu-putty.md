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

## Resumen ejecutivo desde cero

Secuencia completa esperada para manana:

1. Entrar al Ubuntu por PuTTY.
2. Verificar usuario, host y carpeta actual.
3. Crear una carpeta nueva separada: `/opt/inventario-modular`.
4. Clonar desde GitLab la rama `primeros-pasos`.
5. Verificar o instalar Java 21.
6. Compilar con Maven Wrapper.
7. Crear una base MySQL nueva llamada `inventario_modular`.
8. Crear un usuario MySQL propio para la aplicacion.
9. Configurar variables locales sin commitear secretos.
10. Probar el `.jar` solo como laboratorio.
11. Revisar el pipeline de CI/CD en GitLab.

GitHub no es el origen operativo para instalar en Ubuntu. GitHub queda como copia de
seguridad y practica de versionado.

## Paso 1: Conectar por PuTTY

Conectarse al servidor Ubuntu usando la sesion habitual de PuTTY.

Al entrar, verificar usuario y host:

```bash
whoami
hostname
pwd
```

Si el usuario no tiene permisos para `sudo`, pedir a un administrador que ejecute los
pasos que crean carpetas en `/opt`, instalan paquetes o crean base/usuario MySQL.

## Paso 1.1: Confirmar que no se esta tocando el sistema viejo

Antes de crear nada:

```bash
pwd
ls -ld /opt/inventario /opt/inventario-modular 2>/dev/null
```

Regla:

- `/opt/inventario` pertenece al sistema actual.
- `/opt/inventario-modular` sera el nuevo laboratorio Java.
- No ejecutar comandos de prueba dentro de `/opt/inventario`.

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

Si ya existe y no se sabe que contiene, no borrarla. Revisar primero:

```bash
ls -la /opt/inventario-modular
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

Si la carpeta `/opt/inventario-modular` ya fue creada vacia en el paso anterior, usar:

```bash
cd /opt/inventario-modular
git clone --branch primeros-pasos https://gitlab.com/gustavoeliasm/inventario-modular.git .
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

Este paso requiere permisos de administrador en Ubuntu.

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

Si el wrapper falla por permisos de ejecucion, usar siempre `sh ./mvnw ...` como esta
documentado. No hace falta ejecutar `chmod +x` para esta prueba.

## Paso 6: Base de datos

La base definida para el nuevo sistema es:

```text
inventario_modular
```

Importante:

- No usar `inventario_prod`.
- No modificar tablas del inventario actual.
- No correr migraciones contra una base remota sin autorizacion.

Antes de crear la base, entrar a MySQL con el usuario administrador que corresponda:

```bash
sudo mysql
```

o, si el administrador entrega usuario y clave:

```bash
mysql -u root -p
```

Si se autoriza crear una base de laboratorio en MySQL del servidor:

```sql
CREATE DATABASE IF NOT EXISTS inventario_modular
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Si se quiere verificar antes de crear:

```sql
SHOW DATABASES LIKE 'inventario_modular';
```

No usar:

```sql
DROP DATABASE inventario_prod;
DROP DATABASE inventario_modular;
```

Los comandos `DROP` no forman parte de la instalacion inicial.

## Paso 6.1: Crear usuario MySQL de la aplicacion

La aplicacion no debe conectarse como `root`.

Crear usuario propio:

```sql
CREATE USER IF NOT EXISTS 'inventario_modular_app'@'localhost'
  IDENTIFIED BY 'CAMBIAR_EN_EL_SERVIDOR';
```

Dar permisos solo sobre la base nueva:

```sql
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON inventario_modular.* TO 'inventario_modular_app'@'localhost';
FLUSH PRIVILEGES;
```

Verificar permisos:

```sql
SHOW GRANTS FOR 'inventario_modular_app'@'localhost';
```

Salir:

```sql
EXIT;
```

> Nota: reemplazar `CAMBIAR_EN_EL_SERVIDOR` por una clave real solo en la terminal del
> servidor. No escribir esa clave en git, documentacion, chat ni capturas.

Bloque SQL completo para copiar si esta autorizado:

```sql
CREATE DATABASE IF NOT EXISTS inventario_modular
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'inventario_modular_app'@'localhost'
  IDENTIFIED BY 'CAMBIAR_EN_EL_SERVIDOR';
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

Para una prueba temporal en la misma sesion PuTTY, las variables se pueden exportar a mano.
Para una configuracion persistente futura, conviene usar un archivo fuera del repositorio,
por ejemplo:

```text
/etc/inventario-modular/inventario-modular.env
```

No crear este archivo como paso obligatorio todavia si solo se esta haciendo laboratorio.

## Paso 8: Prueba manual sin servicio systemd

Solo para laboratorio, se puede probar el `.jar` sin instalar servicio:

```bash
java -jar target/inventario-modular-0.0.1-SNAPSHOT.jar
```

Si la app intenta conectar a MySQL y faltan variables, puede fallar. Eso es esperable hasta
crear la base y configurar credenciales locales.

No crear servicio systemd todavia. No tocar nginx todavia.

Si se ejecuta con variables en la misma linea:

```bash
INVENTARIO_DB_URL="jdbc:mysql://127.0.0.1:3306/inventario_modular" \
INVENTARIO_DB_USER="inventario_modular_app" \
INVENTARIO_DB_PASSWORD="CAMBIAR_EN_EL_SERVIDOR" \
java -jar target/inventario-modular-0.0.1-SNAPSHOT.jar
```

Esto sigue siendo una prueba manual, no un despliegue productivo.

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
