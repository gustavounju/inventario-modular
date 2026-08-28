# Inventario Modular

Sistema modular de inventario en Java para el Departamento de Informatica del Centro
Judicial San Pedro.

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
