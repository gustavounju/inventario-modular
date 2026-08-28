# CI/CD Para Inventario Modular

Documento de estudio y aplicacion practica de integracion continua y despliegue continuo
para Inventario Modular.

## Objetivo

Usar Inventario Modular como caso real para explicar CI/CD en la facultad y, al mismo
tiempo, dejar una base tecnica util para el proyecto.

## Que significa CI

CI significa **integracion continua**.

En este proyecto, integracion continua significa que cada cambio subido al repositorio se
verifica automaticamente antes de considerarse confiable.

Primeras verificaciones:

- Compilar el proyecto Java.
- Ejecutar tests automatizados.
- Generar reportes de tests.
- Fallar rapido si algo rompe el proyecto.

## Que significa CD

CD puede significar dos cosas:

- **Continuous Delivery**: el sistema deja el artefacto listo para desplegar.
- **Continuous Deployment**: el sistema despliega automaticamente a un ambiente.

Para Inventario Modular, la primera etapa usa **Continuous Delivery**, no despliegue
automatico a produccion.

Motivo:

- El sistema apunta a un entorno institucional.
- Produccion queda fuera durante el inicio.
- Cualquier despliegue real debe ser controlado y reversible.
- Primero necesitamos seguridad, base local y pruebas confiables.

## Pipeline inicial en GitLab

El archivo `.gitlab-ci.yml` define dos etapas:

```text
validar -> construir
```

### Etapa validar

Ejecuta:

```bash
./mvnw --batch-mode --errors --fail-at-end --show-version test
```

Que verifica:

- Java 21 disponible en el runner.
- Dependencias Maven descargables.
- Compilacion del codigo.
- Tests automatizados.

Si esta etapa falla, el cambio no debe considerarse listo.

### Etapa construir

Ejecuta:

```bash
./mvnw --batch-mode --errors --fail-at-end --show-version -DskipTests package
```

Que entrega:

- Un archivo `.jar` generado en `target/`.
- Un artefacto descargable desde GitLab durante 7 dias.

Esta etapa representa la parte de **delivery**: el sistema queda empaquetado y listo para
una futura etapa de despliegue.

## Por que GitLab CI

En este proyecto se decidio:

- GitLab como repositorio principal gestionado automaticamente por el asistente.
- GitHub como repositorio espejo gestionado por Gustavo para practicar comandos Git.

Por eso la primera automatizacion CI/CD se implementa en GitLab.

## Flujo de trabajo

```text
Gustavo o el asistente hacen cambios
  -> se crea un commit
  -> se sube a GitLab
  -> GitLab ejecuta el pipeline
  -> si pasan los tests, se genera el jar
  -> Gustavo sincroniza GitHub con git push github primeros-pasos
```

## Comandos locales equivalentes

Antes de subir cambios, se puede validar localmente:

```powershell
cd "G:\unju2025\google gravity\inventario-modular"
mvn test
mvn -DskipTests package
```

El CI hace lo mismo, pero en un entorno limpio de GitLab.

## Despliegue futuro

La etapa de despliegue todavia no se automatiza.

Cuando llegue el momento, se debera definir:

- Servidor de destino.
- Usuario de despliegue.
- Ruta de instalacion.
- Variables de entorno.
- Conexion segura a MySQL.
- Configuracion LDAP/Active Directory.
- Estrategia de rollback.
- Logs y monitoreo.

## Exposicion sugerida

Idea central para explicar en clase:

```text
CI/CD no es solo subir codigo. Es automatizar controles para saber si el sistema sigue
compilando, si los tests pasan y si existe un artefacto listo para desplegar.
```

En Inventario Modular:

- CI valida que el proyecto Java no este roto.
- CD, en esta primera etapa, genera el `.jar`.
- El despliegue automatico a produccion se deja para una etapa posterior por seguridad.

## Criterios de aceptacion

- Cada push a GitLab ejecuta el pipeline.
- La etapa `validar` corre `mvn test`.
- La etapa `construir` genera un `.jar`.
- Los reportes de tests quedan disponibles en GitLab.
- No se usan secretos reales en `.gitlab-ci.yml`.
- No se conecta a produccion desde CI.
