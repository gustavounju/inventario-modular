# Plan De Trabajo

Plan inicial para construir Inventario Modular Java desde cero.

## Fase 0: Preparacion del entorno

Estado: en curso, parcialmente completada.

Tareas:

- Instalar JDK 21 LTS. Completado.
- Instalar Maven. Completado mediante instalacion local de usuario.
- Verificar Java y Maven en PATH. Completado en la sesion actual.
- Verificar MySQL local en Windows. Completado.
- Crear base local MySQL `inventario_modular` para desarrollo.
- Preparar base `inventario_modular` en el servidor MySQL separado `10.15.0.62` para
  laboratorio/trabajo, solo con autorizacion del DBA/admin.
- Crear repositorio GitLab. Completado.
- Crear repositorio GitHub. Completado por Gustavo.
- Subir rama `primeros-pasos` a GitHub. Completado por Gustavo.
- Documentar cierre de jornada Windows. Completado.
- Documentar runbook Ubuntu por PuTTY para manana. Completado.

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
- Configurar puerto local `8081` mediante `INVENTARIO_SERVER_PORT`, para evitar choque
  con el inventario viejo.
- Configurar conexion MySQL local para Windows.
- Configurar conexion MySQL remota para Ubuntu usando `10.15.0.62`.
- Configurar Flyway.
- Configurar pipeline CI en GitLab para tests y artefacto `.jar`.
- Crear endpoint publico inicial `GET /api/v1/sistema/estado`.
- Crear entrada web administrativa minima en `/admin`.
- Redirigir `/` hacia `/admin`.
- Documentar que HTTPS queda para nginx/reverse proxy en una etapa posterior, porque el
  sistema viejo entraba por HTTPS.

Resultado esperado:

- `mvn test` funciona.
- `mvn spring-boot:run` inicia localmente.
- `/api/v1/sistema/estado` responde estado operativo.
- `/admin` carga una pantalla minima del sistema.
- GitLab CI ejecuta tests y genera un `.jar`.
- No hay dependencia con produccion.

## Fase 2: Modelo de seguridad modular

Estado: siguiente fase recomendada.

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
- Un usuario valido en Active Directory pero no autorizado localmente no puede entrar.
- El administrador puede habilitar usuarios y modulos de forma controlada.

## Fase 3: Active Directory

Estado: primera version completada para login y lectura de atributos; pendiente integrarla
con autorizacion local.

Tareas:

- Configurar LDAP por variables locales. Completado.
- Validar credenciales contra AD. Completado en servidor.
- Manejar error de AD no disponible.
- No guardar claves. Completado en la primera version.
- No loguear claves. Completado en la primera version.
- Mostrar nombre, usuario, fuero y atributos no sensibles desde AD. Completado.

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

Candidato recomendado actualizado: EQUIPOS.

Motivo: Equipos es el nucleo del inventario viejo y alimenta dashboard, tareas, reportes,
mapas, stock asignado y actas. La decision queda condicionada a cerrar primero usuarios y
permisos minimos.

## Fase 7: Migracion progresiva

Tareas futuras:

- Analizar pantallas, tablas y flujos del inventario original.
- Identificar logica funcional que debe conservarse.
- Documentar reglas reales antes de implementar cada modulo.
- Redisenar cada modulo en Java, sin copiar codigo heredado.
- Mantener sistema original funcionando hasta validar reemplazo.

Resultado esperado:

- El inventario viejo queda como referencia de negocio.
- El inventario nuevo queda como implementacion limpia en Java.
- Cada modulo migrado tiene reglas documentadas, permisos definidos y pruebas basicas.

## Riesgos conocidos

- Configuracion real de Active Directory incompleta o cambiante.
- Diferencias entre red de casa y red del trabajo.
- Base MySQL del trabajo ubicada en otro servidor (`10.15.0.62`), no en el Ubuntu de la app.
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

Ver tambien: [Versionado Git](./versionado-git.md).

## Proxima accion recomendada

El servidor Ubuntu ya levanta Inventario Modular como servicio `systemd`, con MySQL remoto
y login AD. La proxima accion recomendada es implementar autorizacion local minima:

```text
AD autentica identidad.
Inventario Modular autoriza acceso y modulos.
```

Despues de esa capa, comenzar con el modulo EQUIPOS.

Ver tambien:

- [Runbook Ubuntu por PuTTY](./runbook-manana-ubuntu-putty.md)
- [Proximo paso funcional](./proximo-paso-funcional.md)

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

Si ambos comandos funcionan, crear la base local MySQL `inventario_modular` para
desarrollo y continuar con las migraciones Flyway iniciales. Para Ubuntu/trabajo, seguir
el runbook y no asumir MySQL local.
