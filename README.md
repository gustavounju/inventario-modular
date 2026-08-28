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

## Repositorios

- GitLab: `https://gitlab.com/gustavoeliasm/inventario-modular`
- GitHub: `https://github.com/gustavounju/inventario-modular`

Decision de trabajo:

- GitLab lo puede gestionar el asistente de forma automatica cuando haga falta.
- GitHub lo gestiona Gustavo por comandos para practicar versionado.

La rama inicial `primeros-pasos` ya fue subida a ambos remotos.
