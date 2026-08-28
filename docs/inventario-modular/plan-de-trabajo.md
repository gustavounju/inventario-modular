# Plan De Trabajo

Plan inicial para construir Inventario Modular Java desde cero.

## Fase 0: Preparacion del entorno

Estado: en curso, parcialmente completada.

Tareas:

- Instalar JDK 21 LTS. Completado.
- Instalar Maven. Completado mediante instalacion local de usuario.
- Verificar Java y Maven en PATH. Completado en la sesion actual.
- Verificar MySQL local. Completado.
- Crear base local MySQL `inventario_modular`.
- Crear repositorio GitLab. Completado.
- Crear repositorio GitHub. Completado por Gustavo.
- Subir rama `primeros-pasos` a GitHub. Completado por Gustavo.

Resultado esperado:

- La maquina puede compilar y ejecutar proyectos Spring Boot modernos.
- El paquete Java base queda alineado con `justiciajujuy.gov.ar` y San Pedro:
  `ar.gov.justiciajujuy.sanpedro.inventario`.

## Fase 1: Proyecto base

Tareas:

- Crear carpeta limpia `inventario-modular`.
- Crear proyecto Spring Boot.
- Configurar estructura de paquetes.
- Configurar perfiles `local`, `test` y futuro `prod`.
- Configurar conexion MySQL local.
- Configurar Flyway.

Resultado esperado:

- `mvn test` funciona.
- `mvn spring-boot:run` inicia localmente.
- No hay dependencia con produccion.

## Fase 2: Modelo de seguridad modular

Tareas:

- Crear tablas de usuarios internos.
- Crear tablas de roles.
- Crear tablas de permisos.
- Crear tablas de modulos.
- Crear relacion usuario-rol.
- Crear relacion rol-permiso-modulo.
- Crear seed inicial de modulos.

Resultado esperado:

- El sistema puede representar que modulos ve cada usuario y que acciones puede realizar.

## Fase 3: Active Directory

Tareas:

- Configurar LDAP por variables locales.
- Validar credenciales contra AD.
- Manejar error de AD no disponible.
- No guardar claves.
- No loguear claves.

Resultado esperado:

- La identidad se valida contra dominio y la autorizacion se resuelve en MySQL.

## Fase 4: API-first

Tareas:

- Crear API versionada `/api/v1`.
- Crear endpoint de login.
- Crear endpoint de usuario actual.
- Crear endpoint de modulos permitidos.
- Crear respuestas 401 y 403 consistentes.

Resultado esperado:

- Una futura app movil puede consumir el nucleo sin depender de pantallas HTML.

## Fase 5: Panel administrativo minimo

Tareas:

- Crear login visual.
- Crear pantalla de usuarios.
- Crear pantalla de roles.
- Crear pantalla de permisos por modulo.
- Crear pantalla de modulos activables.

Resultado esperado:

- Un administrador puede dar acceso real a usuarios de dominio.

## Fase 6: Primer modulo funcional

Candidato recomendado: ACTAS o MUEBLES.

La decision queda pendiente hasta terminar seguridad y permisos.

## Fase 7: Migracion progresiva

Tareas futuras:

- Analizar tablas del inventario original.
- Identificar logica funcional que debe copiarse.
- Migrar por modulo.
- Mantener sistema original funcionando hasta validar reemplazo.

## Riesgos conocidos

- Configuracion real de Active Directory incompleta o cambiante.
- Diferencias entre red de casa y red del trabajo.
- Permisos institucionales para consultar AD.
- Instaladores bloqueados por politicas del trabajo.
- Intentar migrar demasiados modulos antes de cerrar seguridad.

## Convencion de versionado

- GitLab puede ser gestionado automaticamente por el asistente.
- GitHub lo gestiona Gustavo por comandos para practicar versionado.
- Los commits nuevos deben escribirse en español latino.
- GitLab y GitHub deben quedar con la misma rama `primeros-pasos` y los mismos commits.

Comando de sincronizacion manual hacia GitHub:

```powershell
cd "G:\unju2025\google gravity\inventario-modular"
git push github primeros-pasos
```

## Proxima accion recomendada

El entorno documentado para estos pasos es Windows con MySQL local.

Si Maven no aparece en una PowerShell nueva, verificar que el Path de usuario tenga:

```text
C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin
C:\Users\Gustavo\tools\apache-maven-3.9.16\bin
```

Luego verificar:

```powershell
java -version
mvn -version
```

Si ambos comandos funcionan, crear la base local MySQL `inventario_modular` y continuar
con las migraciones Flyway iniciales.
