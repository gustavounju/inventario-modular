# Runbook Para Manana: Ubuntu Por PuTTY

Guia para continuar en el trabajo desde el servidor Ubuntu, usando PuTTY/SSH.

## Alcance

Este runbook es para preparar una instalacion de laboratorio/staging del nuevo
Inventario Modular Java en el servidor Ubuntu o en una ubicacion separada autorizada.

No reemplaza el inventario actual. No toca la base `inventario_prod`. No reinicia servicios
existentes.

Dato importante de infraestructura: la base MySQL del trabajo no esta en el mismo servidor
Ubuntu donde va a correr la aplicacion. La base esta en el servidor separado
`10.15.0.62`.
Por eso, en el servidor Ubuntu de la app no se debe asumir `localhost` para MySQL.

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
7. Confirmar conectividad desde el servidor Ubuntu de la app hacia MySQL en `10.15.0.62`.
8. Crear una base MySQL nueva llamada `inventario_modular` en `10.15.0.62`, si el DBA/admin lo autoriza.
9. Crear un usuario MySQL propio para la aplicacion, autorizado desde el servidor Ubuntu de la app.
10. Configurar variables locales sin commitear secretos.
11. Probar el `.jar` solo como laboratorio.
12. Revisar el pipeline de CI/CD en GitLab.

GitHub no es el origen operativo para instalar en Ubuntu. GitHub queda como copia de
seguridad y practica de versionado.

## Estado preparado antes de continuar

Acciones ya verificadas el 2026-08-28:

- Se creo una copia local en Windows en:

```text
C:\Users\gmurad\Documents\ChatGPT\inventario-modular
```

- La copia local quedo en rama `primeros-pasos`.
- Los remotos locales quedaron alineados con la decision del proyecto:

```text
origin -> https://gitlab.com/gustavoeliasm/inventario-modular.git
github -> https://github.com/gustavounju/inventario-modular.git
```

- En Windows se verifico Java 21 con `JAVA_HOME` apuntando temporalmente a:

```text
C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot
```

- Se ejecuto `.\mvnw.cmd --batch-mode test` y termino con `BUILD SUCCESS`.
- Se ejecuto `.\mvnw.cmd --batch-mode -DskipTests package` y genero:

```text
target\inventario-modular-0.0.1-SNAPSHOT.jar
```

- El `.jar` local generado midio aproximadamente 67 MB.
- El estado de Git quedo limpio antes de documentar esta bitacora.

## Aviso permanente sobre CI/CD

Cada vez que durante la instalacion aparezca una oportunidad concreta para revisar,
explicar o mejorar CI/CD, hay que avisarlo antes de seguir. Puntos obligatorios:

- Despues de clonar desde GitLab, revisar `.gitlab-ci.yml`.
- Despues de confirmar el ultimo commit, revisar en GitLab `Build -> Pipelines`.
- Confirmar que las etapas esperadas sean `validar` y `construir`.
- Confirmar que `validar` corre tests y que `construir` genera el `.jar`.
- No agregar secretos ni conexiones a MySQL real en CI sin una politica formal.

## Paso 1: Conectar por PuTTY

Conectarse al servidor Ubuntu usando la sesion habitual de PuTTY.

Al entrar, verificar usuario y host:

```bash
whoami
hostname
pwd
```

Si el usuario no tiene permisos para `sudo`, pedir a un administrador que ejecute los
pasos que crean carpetas en `/opt` o instalan paquetes.

La creacion de base/usuario MySQL probablemente no se hace en este Ubuntu, sino en el
servidor de base `10.15.0.62` o por el DBA/admin responsable de ese servidor.

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

### Paso 3.1: Acceso SSH a GitLab desde el servidor Ubuntu

Si el clone por HTTPS pide usuario y clave, recordar que GitLab no acepta la clave normal
de la cuenta para operaciones Git por HTTPS. Para el servidor Ubuntu conviene usar SSH con
una clave propia del servidor, o un Personal Access Token si se decide mantener HTTPS.

Accion realizada el 2026-08-28 en `serverinventario`:

- Se confirmo que el servidor podia llegar a `gitlab.com`.
- Se acepto la huella ED25519 de `gitlab.com` en `~/.ssh/known_hosts`.
- El intento `ssh -T git@gitlab.com` fallo con `Permission denied (publickey)`, porque el
  usuario `administrador` todavia no tenia una clave autorizada en GitLab.
- Se genero una clave SSH ED25519 para GitLab en:

```text
~/.ssh/id_ed25519_gitlab
~/.ssh/id_ed25519_gitlab.pub
```

Comando usado:

```bash
ssh-keygen -t ed25519 -C "serverinventario gitlab inventario-modular" -f ~/.ssh/id_ed25519_gitlab
```

La clave publica debe agregarse en GitLab, dentro de **User Settings -> SSH Keys**. La
clave privada `~/.ssh/id_ed25519_gitlab` no debe mostrarse, copiarse al repo ni compartirse.

Despues de agregar la clave publica en GitLab, configurar SSH para que use esa clave con
`gitlab.com`:

```bash
cat > ~/.ssh/config <<'EOF'
Host gitlab.com
  HostName gitlab.com
  User git
  IdentityFile ~/.ssh/id_ed25519_gitlab
  IdentitiesOnly yes
EOF

chmod 600 ~/.ssh/config
ssh -T git@gitlab.com
```

Si GitLab responde con un mensaje de bienvenida, clonar con la URL SSH:

```bash
cd /opt/inventario-modular
git clone --branch primeros-pasos git@gitlab.com:gustavoeliasm/inventario-modular.git .
```

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
- No correr migraciones contra `10.15.0.62` sin autorizacion.
- No asumir que MySQL esta en `localhost`; para el trabajo la base esta en `10.15.0.62`.

### Paso 6.0: Confirmar conectividad hacia `10.15.0.62`

Desde el servidor Ubuntu de la aplicacion:

```bash
ping -c 4 10.15.0.62
```

Luego probar el puerto MySQL:

```bash
nc -vz 10.15.0.62 3306
```

Si `nc` no esta instalado, usar esta prueba alternativa:

```bash
timeout 5 bash -c '</dev/tcp/10.15.0.62/3306' && echo "MySQL alcanzable" || echo "No conecta"
```

Si no conecta, no seguir con la prueba de la app. Hay que confirmar con el administrador:

- Confirmacion de que `10.15.0.62` es la IP correcta del servidor MySQL.
- Puerto MySQL, normalmente `3306`.
- Firewall entre el servidor Ubuntu de la app y `10.15.0.62`.
- Usuario MySQL permitido desde la IP del servidor Ubuntu de la app.

### Paso 6.1: Crear la base en el servidor correcto

La base se crea en `10.15.0.62`, no necesariamente desde el Ubuntu de la aplicacion.

Si se esta conectado directamente al servidor de base `10.15.0.62` por consola autorizada:

```bash
sudo mysql
```

Si el administrador autoriza conectarse remotamente desde el servidor Ubuntu de la app:

```bash
mysql -h 10.15.0.62 -u root -p
```

Si el DBA/admin entrega otro usuario administrador, reemplazar `root` por ese usuario.

Crear solamente la base nueva:

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

## Paso 6.2: Crear usuario MySQL de la aplicacion

La aplicacion no debe conectarse como `root`.

Como la app corre en otro servidor, el usuario MySQL debe quedar autorizado desde el
servidor Ubuntu de la aplicacion, no desde `localhost`.

Primero identificar o pedir al administrador la IP/nombre del servidor Ubuntu donde correra
la app. En el ejemplo se usa el placeholder `IP_DEL_SERVIDOR_APP`.

Crear usuario propio:

```sql
CREATE USER IF NOT EXISTS 'inventario_modular_app'@'IP_DEL_SERVIDOR_APP'
  IDENTIFIED BY 'CAMBIAR_EN_EL_SERVIDOR';
```

Dar permisos solo sobre la base nueva:

```sql
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON inventario_modular.* TO 'inventario_modular_app'@'IP_DEL_SERVIDOR_APP';
FLUSH PRIVILEGES;
```

Verificar permisos:

```sql
SHOW GRANTS FOR 'inventario_modular_app'@'IP_DEL_SERVIDOR_APP';
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
CREATE USER IF NOT EXISTS 'inventario_modular_app'@'IP_DEL_SERVIDOR_APP'
  IDENTIFIED BY 'CAMBIAR_EN_EL_SERVIDOR';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON inventario_modular.* TO 'inventario_modular_app'@'IP_DEL_SERVIDOR_APP';
FLUSH PRIVILEGES;
```

No commitear esa clave. No pegarla en documentacion.

Evitar `'inventario_modular_app'@'%'` salvo que el DBA/admin lo decida explicitamente.
Es mas seguro autorizar solo la IP o nombre del servidor Ubuntu de la aplicacion.

## Paso 7: Configuracion local del servidor

La configuracion real debe quedar fuera de git.

Variables esperadas:

```bash
export INVENTARIO_DB_URL="jdbc:mysql://10.15.0.62:3306/inventario_modular"
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
crear la base en `10.15.0.62`, autorizar el usuario y configurar credenciales locales.

No crear servicio systemd todavia. No tocar nginx todavia.

Si se ejecuta con variables en la misma linea:

```bash
INVENTARIO_DB_URL="jdbc:mysql://10.15.0.62:3306/inventario_modular" \
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
- El servidor Ubuntu alcanza `10.15.0.62:3306`, o queda registrado que falta habilitar red/firewall.
- Se puede ver el pipeline en GitLab.
- No se toca el inventario actual ni la base `inventario_prod`.

## Bitacora de instalacion en Ubuntu

### 2026-08-28 - Preparacion inicial por PuTTY

Sesion observada:

```text
usuario: administrador
host: serverinventario
carpeta inicial: /home/administrador
```

Verificaciones realizadas:

- `/opt/inventario` existe y pertenece al sistema actual. No se debe usar para esta prueba.
- `/opt/inventario-modular` no existia al iniciar.
- Se creo `/opt/inventario-modular` como carpeta separada para laboratorio:

```bash
sudo mkdir -p /opt/inventario-modular
sudo chown "$USER":"$USER" /opt/inventario-modular
```

Resultado:

```text
drwxr-xr-x 2 administrador administrador 4096 ago 28 08:13 /opt/inventario-modular
```

Intento de clone por HTTPS:

```bash
cd /opt/inventario-modular
git clone --branch primeros-pasos https://gitlab.com/gustavoeliasm/inventario-modular.git .
```

Resultado:

- GitLab pidio usuario y clave.
- La autenticacion fallo con `HTTP Basic: Access denied`.
- Se decidio mantener GitLab como origen obligatorio y configurar SSH en lugar de clonar
  desde GitHub.

Verificacion SSH:

```bash
ls -la ~/.ssh
ssh -T git@gitlab.com
```

Resultado:

- Existia `~/.ssh`, pero no habia clave privada autorizada para GitLab.
- Se acepto la huella ED25519 de `gitlab.com`.
- GitLab respondio `Permission denied (publickey)`.

Clave SSH generada para GitLab:

```bash
ssh-keygen -t ed25519 -C "serverinventario gitlab inventario-modular" -f ~/.ssh/id_ed25519_gitlab
```

Clave publica mostrada para cargar en GitLab:

```text
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIKfSYO8vUF30k7YNuxFAcK1EMbHgMtnd/mhrWiWYO5Qz serverinventario gitlab inventario-modular
```

Pendiente inmediato:

1. Agregar la clave publica en GitLab, en **User Settings -> SSH Keys**.
2. Crear `~/.ssh/config` para usar `~/.ssh/id_ed25519_gitlab` con `gitlab.com`.
3. Probar `ssh -T git@gitlab.com`.
4. Si la prueba responde bienvenida de GitLab, clonar con:

```bash
cd /opt/inventario-modular
git clone --branch primeros-pasos git@gitlab.com:gustavoeliasm/inventario-modular.git .
```

Nota de seguridad: la clave privada `~/.ssh/id_ed25519_gitlab` no debe compartirse ni
guardarse en el repositorio.
