# Instalacion Desde Cero

Guia para preparar una maquina **Windows** de desarrollo para Inventario Modular Java.

Esta guia no describe una instalacion Linux ni una instalacion de produccion. El alcance
es una maquina Windows de desarrollo, con MySQL local y sin conexion a la base remota del
Centro Judicial.

## Estado actual observado

En la maquina de desarrollo se detecto:

- Java inicial en PATH: OpenJDK 8 de Red Hat.
- JDK 21 instalado correctamente con winget.
- Maven instalado localmente en el perfil del usuario.
- Cliente `mysql` no disponible en PATH.
- MySQL local escuchando en `127.0.0.1:3306`.
- `winget` disponible.
- Chocolatey disponible.

## Paso 1: Instalar JDK 21 LTS

Usar JDK 21 LTS, no JDK 22. Java 21 es moderno, estable y adecuado para Spring Boot en un
entorno institucional. Java 22 fue una version intermedia, no una base recomendable para
un sistema que se espera mantener durante anos.

Comando:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

Salida esperada aproximada:

```text
Encontrado Eclipse Temurin JDK with Hotspot 21
Descargando OpenJDK21U-jdk_x64_windows_hotspot...
El hash del instalador se verifico correctamente
Instalado correctamente
```

Despues de instalar, cerrar y abrir PowerShell.

Verificar:

```powershell
java -version
javac -version
```

Debe aparecer Java 21.

## Paso 2: Instalar Maven

### Intento con Chocolatey

El comando `winget install Apache.Maven` no funciono en esta maquina porque `winget` no
encontro un paquete coincidente. Como Chocolatey si esta instalado y el paquete `maven`
existe, el comando recomendado es:

```powershell
choco install maven -y
```

En esta maquina, Chocolatey fallo porque PowerShell no estaba ejecutandose como
administrador y no pudo escribir en:

```text
C:\ProgramData\chocolatey\lib-bad
```

Ese error no significa que Maven sea incorrecto. Significa que esa instalacion necesita
PowerShell como administrador o una instalacion local sin privilegios.

### Instalacion local sin administrador

Se instalo Maven de forma local en el perfil del usuario:

```text
C:\Users\Gustavo\tools\apache-maven-3.9.16
```

Variables de usuario configuradas:

```text
JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot
MAVEN_HOME=C:\Users\Gustavo\tools\apache-maven-3.9.16
```

Entradas agregadas al Path de usuario:

```text
C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin
C:\Users\Gustavo\tools\apache-maven-3.9.16\bin
```

Despues de instalar, cerrar y abrir PowerShell.

Verificar:

```powershell
mvn -version
```

Debe aparecer Maven 3.9.x o superior.

Salida verificada en esta maquina:

```text
openjdk version "21.0.12.1" 2026-08-18 LTS
Apache Maven 3.9.16
Maven home: C:\Users\Gustavo\tools\apache-maven-3.9.16
Java version: 21.0.12.1
```

## Alternativa si Chocolatey esta bloqueado

Si en una maquina del trabajo no se permite instalar Maven con Chocolatey:

1. Descargar Apache Maven desde el sitio oficial.
2. Descomprimirlo en una carpeta estable, por ejemplo:

```text
C:\tools\apache-maven
```

3. Crear o actualizar la variable de entorno `MAVEN_HOME`:

```text
C:\tools\apache-maven
```

4. Agregar al `Path`:

```text
C:\tools\apache-maven\bin
```

5. Cerrar y abrir PowerShell.
6. Verificar:

```powershell
mvn -version
```

## Paso 3: Verificar MySQL local

Comprobar que MySQL local responde:

```powershell
Test-NetConnection -ComputerName 127.0.0.1 -Port 3306
```

Debe indicar:

```text
TcpTestSucceeded : True
```

## Paso 4: Crear base local nueva

Produccion queda fuera. La primera base es local de desarrollo y debe estar en MySQL.

Nombre de base:

```text
inventario_modular
```

Cuando el cliente `mysql` este disponible:

```powershell
mysql -u root -p
```

Dentro de MySQL:

```sql
CREATE DATABASE inventario_modular
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

## Paso 5: Crear el proyecto Java

El proyecto debe crearse en un directorio limpio:

```text
inventario-modular/
```

Stack recomendado:

- Java 21 LTS
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- Spring LDAP
- MySQL Driver
- Flyway para migraciones
- Validation
- Thymeleaf solo para panel administrativo minimo

## Paso 6: Variables locales

Crear configuracion local con placeholders. No commitear secretos.

Valores esperados:

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/inventario_modular
spring.datasource.username=usuario_local
spring.datasource.password=CAMBIAR_EN_LOCAL

inventario.ldap.url=ldap://SERVIDOR_AD:389
inventario.ldap.domain=DOMINIO
inventario.ldap.base-dn=DC=ejemplo,DC=local
```

Las credenciales reales deben quedar fuera de git.

## Paso 7: Primer arranque esperado

Cuando el proyecto exista:

```powershell
mvn spring-boot:run
```

El sistema debe iniciar localmente sin conectarse a produccion.

## Pendiente antes de implementar

- Confirmar servidor, puerto y base DN reales de Active Directory.
- Confirmar si se usara LDAP simple, LDAPS o StartTLS.
- Definir usuario administrador inicial.
- Crear migraciones Flyway iniciales.
- Crear pruebas de login y autorizacion.

## Repositorios remotos

El proyecto nuevo debe vivir como repositorio propio, no como rama del inventario viejo.

Repositorio GitLab creado:

```text
https://gitlab.com/gustavoeliasm/inventario-modular
```

Rama inicial:

```text
primeros-pasos
```

Repositorio GitHub:

```text
https://github.com/gustavounju/inventario-modular
```

Decision de operacion:

- GitLab puede ser gestionado automaticamente por el asistente.
- GitHub lo gestiona Gustavo por comandos para practicar versionado.

Comando usado para subir a GitHub:

```powershell
git push -u github primeros-pasos
```

Salida esperada cuando sube correctamente:

```text
[new branch]      primeros-pasos -> primeros-pasos
branch 'primeros-pasos' set up to track 'github/primeros-pasos'
```

## Nota de aprendizaje GitHub CLI

Si se ejecuta:

```powershell
gh repo create gustavounju/inventario-modular --private --source . --push
```

y GitHub responde:

```text
GraphQL: Name already exists on this account (createRepository)
```

significa que el repositorio ya existe en esa cuenta. En ese caso no se debe crear de
nuevo; corresponde usar `git push` hacia el remoto existente.
