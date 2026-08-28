# Inventario Modular

Sistema modular de inventario en Java para el Departamento de Informatica del Centro
Judicial San Pedro.

## Entorno objetivo inicial

Esta instalacion y este repositorio estan documentados para **Windows** como entorno de
desarrollo inicial.

La base de datos de desarrollo es **MySQL local**, separada de cualquier base de
produccion:

```text
inventario_modular
```

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
mvn test
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
mvn spring-boot:run
```

## Base de datos local

La base local de desarrollo sera:

```text
inventario_modular
```

Produccion queda fuera de esta primera etapa.

## Documentacion

La documentacion del estudio inicial esta en `docs/inventario-modular`.

Documentos principales:

- [Instalacion desde cero](docs/inventario-modular/instalacion-desde-cero.md)
- [Requerimientos del sistema](docs/inventario-modular/requerimientos-sistema.md)
- [Plan de trabajo](docs/inventario-modular/plan-de-trabajo.md)
- [Versionado Git](docs/inventario-modular/versionado-git.md)
- [CI/CD](docs/inventario-modular/ci-cd.md)
- [Cierre de jornada Windows](docs/inventario-modular/cierre-jornada-windows.md)
- [Runbook Ubuntu por PuTTY](docs/inventario-modular/runbook-manana-ubuntu-putty.md)

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
cd "G:\unju2025\google gravity\inventario-modular"
git push github primeros-pasos
```
