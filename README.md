# Inventario Modular

Sistema modular de inventario en Java para el Departamento de Informatica del Centro
Judicial San Pedro.

## Entorno objetivo inicial

Esta instalacion y este repositorio estan documentados para **Windows** como entorno de
desarrollo inicial.

La base de datos de desarrollo en Windows puede ser **MySQL local**, separada de cualquier
base de produccion:

```text
inventario_modular
```

En el entorno del trabajo, la base MySQL no esta en el mismo servidor Ubuntu de la
aplicacion. Debe apuntar al servidor separado `10.15.0.62`, con permisos otorgados al
host/IP del servidor de aplicacion.

## Enfoque

Inventario Modular nace como un backend **API-first** preparado para una futura app movil.
La web administrativa sera un cliente minimo para configuracion y gestion, no el lugar
principal de las reglas de negocio.

## Stack

- Java 21 LTS
- Spring Boot
- Maven
- MySQL
- Flyway
- Spring Security
- LDAP / Active Directory
- Thymeleaf para panel administrativo minimo

## Paquete Java

La estructura base del codigo sigue el dominio institucional
`justiciajujuy.gov.ar`, invertido segun la convencion Java, y agrega la sede San Pedro
porque esta instalacion corresponde a San Pedro de Jujuy:

```text
src/main/java/ar/gov/justiciajujuy/sanpedro/inventario
```

Paquete base:

```java
ar.gov.justiciajujuy.sanpedro.inventario
```

## Rama inicial

La rama de arranque del proyecto es:

```text
primeros-pasos
```

## Desarrollo local

Verificar entorno:

```powershell
java -version
mvn -version
```

En esta maquina, Maven quedo instalado localmente en:

```text
C:\Users\Gustavo\tools\apache-maven-3.9.16
```

Ejecutar tests:

```powershell
.\mvnw.cmd --batch-mode test
```

Si `mvn` no aparece o Maven usa Java 8:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:USERPROFILE\tools\apache-maven-3.9.16\bin;$env:Path"
mvn test
```

`BUILD SUCCESS` es una salida de Maven, no un comando.

Ejecutar app:

```powershell
.\mvnw.cmd spring-boot:run
```

Por defecto el perfil local usa el puerto `8081` para no chocar con el inventario viejo:

```text
http://localhost:8081/admin
http://localhost:8081/api/v1/sistema/estado
```

`/admin` requiere autenticacion. En laboratorio local, si Active Directory no esta
activado, Spring Security muestra el formulario de login y genera un usuario temporal de
desarrollo. En Ubuntu, la autenticacion prevista es contra Active Directory.

El inventario viejo entraba por HTTPS. En Inventario Modular, durante laboratorio, la app
Spring Boot corre por HTTP interno y queda preparada para recibir HTTPS delante mediante
nginx/reverse proxy cuando se defina el despliegue real. No instalar nginx ni systemd para
este laboratorio inicial.

## Base de datos

La base de desarrollo/local y la futura base de laboratorio en el trabajo se llaman:

```text
inventario_modular
```

En Windows puede correr contra MySQL local para estudiar y probar. En el trabajo, la app
Ubuntu debe conectarse a MySQL en `10.15.0.62`, no a `localhost`. Produccion queda fuera
de esta primera etapa.

## Active Directory

La primera integracion de seguridad autentica usuarios contra Active Directory en modo
solo lectura. La app no crea, modifica ni borra usuarios del dominio.

Variables previstas fuera de git:

```text
INVENTARIO_LDAP_ENABLED=true
INVENTARIO_LDAP_URL=ldap://SERVIDOR_AD:389
INVENTARIO_LDAP_DOMAIN=DOMINIO
INVENTARIO_LDAP_BASE_DN=DC=ejemplo,DC=local
INVENTARIO_LDAP_DISPLAY_NAME_ATTRIBUTE=displayName
INVENTARIO_LDAP_FUERO_ATTRIBUTE=department
```

Al iniciar sesion, el panel `/admin` muestra:

- Usuario/cuenta usada para autenticarse.
- Nombre visible traido del atributo `displayName`.
- Fuero traido del atributo configurable `INVENTARIO_LDAP_FUERO_ATTRIBUTE`.
- Tabla de atributos no sensibles recibidos desde AD durante el login.
- Boton `Salir`, conectado al logout de Spring Security.

Antes de activar AD en Ubuntu hay que confirmar con Sistemas/AD los valores reales de
dominio, base DN y el atributo exacto donde esta cargado el fuero.

Guia tecnica:

- [Login Active Directory](docs/inventario-modular/login-active-directory.md)
- [ADR-004: Login Active Directory solo lectura](docs/decisions/ADR-004-login-active-directory-solo-lectura.md)

## Documentacion

La documentacion del estudio inicial esta en `docs/inventario-modular`.

Documentos principales:

- [Instalacion desde cero](docs/inventario-modular/instalacion-desde-cero.md)
- [Requerimientos del sistema](docs/inventario-modular/requerimientos-sistema.md)
- [Plan de trabajo](docs/inventario-modular/plan-de-trabajo.md)
- [Versionado Git](docs/inventario-modular/versionado-git.md)
- [CI/CD](docs/inventario-modular/ci-cd.md)
- [Login Active Directory](docs/inventario-modular/login-active-directory.md)
- [Proximo paso funcional](docs/inventario-modular/proximo-paso-funcional.md)
- [Cierre de jornada Windows](docs/inventario-modular/cierre-jornada-windows.md)
- [Runbook Ubuntu por PuTTY](docs/inventario-modular/runbook-manana-ubuntu-putty.md)
- [Modo local Windows sin dominio](docs/inventario-modular/modo-local-windows-sin-dominio.md)
- [Base de datos local Windows](docs/inventario-modular/base-datos-local-windows.md)

## Repositorios

- GitLab: `https://gitlab.com/gustavoeliasm/inventario-modular`
- GitHub: `https://github.com/gustavounju/inventario-modular`

Decision de trabajo:

- GitLab lo puede gestionar el asistente de forma automatica cuando haga falta.
- GitHub lo gestiona Gustavo por comandos para practicar versionado.
- Desde esta decision, los commits nuevos se escriben en español latino.

La rama inicial `primeros-pasos` ya fue subida a ambos remotos.

Para subir a GitHub los commits que el asistente ya subio a GitLab:

```powershell
cd "C:\Users\gmurad\Documents\ChatGPT\inventario-modular"
git push github primeros-pasos
```
