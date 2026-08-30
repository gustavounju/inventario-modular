# Proximo paso funcional

## Estado actual

Inventario Modular ya tiene una primera base real en el servidor Ubuntu:

- Repositorio en `/opt/inventario-modular`.
- Servicio `inventario-modular.service` creado en `systemd`.
- Arranque automatico habilitado con `systemctl enable`.
- Configuracion real fuera de git en `/etc/inventario-modular/inventario-modular.env`.
- Conexion a MySQL remoto `10.15.0.62`.
- Login contra Active Directory `10.15.0.41`.
- Pantalla `/admin` protegida por login.
- Nombre, usuario, fuero y atributos AD visibles en modo solo lectura.

## Lectura desde el sistema viejo

Las funciones principales observadas en el inventario viejo son:

- Dashboard de PCs/equipos.
- Recepcion de datos desde scripts de inventario.
- Cruce de usuario de sesion con usuarios AD.
- Fuero/area de equipos.
- Tareas tecnicas.
- Stock y componentes.
- Actas y reportes.
- Mapas/ubicaciones.
- Usuarios, roles y permisos locales.
- Mantenimiento y sincronizacion AD.

## Decision recomendada

Antes de migrar un modulo funcional grande, conviene cerrar una capa minima de
autorizacion local.

Motivo:

```text
Active Directory autentica identidad.
Inventario Modular debe decidir autorizacion.
```

Hoy el sistema ya puede validar un usuario de AD, autorizarlo localmente, asignarle roles
iniciales y mostrar u ocultar accesos por permiso.

## Primer sprint iniciado: Usuarios y permisos minimos

Estado: primera capa implementada; administracion visual inicial completada.

Objetivo:

- Crear tabla de usuarios autorizados. Completado.
- Crear tabla de roles. Completado.
- Crear tabla de modulos. Completado.
- Crear permisos minimos por modulo. Completado.
- Dejar un usuario administrador inicial. Completado con `admin.local`.
- Exponer datos de usuario y modulos por API. Completado con `/api/v1/me` y
  `/api/v1/me/modulos`.
- Mostrar modulos habilitados en `/admin`. Completado.
- Login local repetido en Windows/casa. Corregido.
- Permitir que solo usuarios autorizados localmente entren al panel. Pendiente de aplicar
  despues de cargar un administrador real de dominio.
- Crear pantalla administrativa para usuarios, roles y modulos. Primera version
  completada con `/admin/usuarios`.
- Separar identidad, autenticacion y autorizacion. Documentado en ADR-007.
- Crear usuarios locales con password propio y hash BCrypt. Primera version completada.
- Buscar/listar usuarios de Active Directory en produccion. Primera version completada
  con lectura LDAP opcional.

Resultado esperado:

- Usuario valido en AD pero no autorizado localmente: queda como `Pendiente de
  autorizacion` y sin modulos.
- Usuario autorizado: entra y ve sus modulos.
- Administrador: puede ver todo.
- No se guardan claves de AD.

Este sprint es pequeno pero muy importante porque evita construir modulos encima de una
seguridad incompleta.

## Sprint completado parcialmente

### Sprint 2: Administracion visual de usuarios, roles y modulos

Antes de migrar Equipos convenia terminar la pantalla que permita administrar la seguridad
modular desde el navegador. La primera version ya existe y permite separar alta local de
autorizacion AD.

Motivo:

- El sistema ya tiene tablas y API para usuarios, roles, permisos y modulos.
- Falta una interfaz para que el administrador no dependa de consultas SQL.
- Cada modulo futuro necesita saber si el usuario puede verlo o administrarlo.
- La regla debe quedar visible y facil de estudiar: AD autentica, MySQL autoriza.

Primer alcance del sprint:

- Pantalla `/admin/usuarios`. Completado.
- Listado de identidades autorizadas y pendientes. Completado.
- Formulario para autorizar una identidad con rol inicial. Completado.
- Selector de rol inicial. Completado.
- Tests de controlador para proteger el flujo. Completado.
- Busqueda/listado de usuarios de Active Directory en produccion. Primera version
  completada.
- Edicion de roles de usuarios existentes. Pendiente.
- Activar o desactivar usuarios existentes. Pendiente.
- Cambio de clave para usuarios locales. Pendiente.
- Auditoria de altas, cambios de clave y desactivaciones. Pendiente.
- Vista de modulos y permisos que recibe cada rol. Pendiente.

## Sprint iniciado

### Sprint 3: Equipos como primer modulo funcional

El primer modulo funcional iniciado es `Equipos`.

Motivo:

- Es el corazon del inventario viejo.
- Alimenta dashboard, tareas, reportes, mapas, stock asignado y actas.
- Ya existen reglas reales: nombre de PC, ultimo usuario, fuero, IP, sistema operativo,
  procesador, RAM, impresora, monitoreo y estado activo.
- Permite probar desde temprano el enfoque API-first.

Primer alcance de Equipos:

- Migracion Flyway para tabla `equipos`. Completado.
- Endpoint `GET /api/v1/equipos`. Completado.
- Endpoint `GET /api/v1/equipos/{id}`. Completado.
- Endpoint interno para recibir inventario de una PC. Completado como
  `POST /api/v1/equipos/inventario`.
- Pantalla simple de listado. Completado en `/admin/equipos`.
- Busqueda por nombre, usuario o fuero. Completado.
- Tests de controlador/pantalla. Completado.
- Conectar el script real `inventario.ps1`. Pendiente.
- Definir autenticacion de maquina o token especifico para reportes automaticos.
  Pendiente.
- Importar datos iniciales desde el inventario viejo. Pendiente.

## No comenzar todavia por

### Stock

Tiene mas reglas de ciclo de vida, asignaciones, remitos, compras y movimientos. Conviene
tener primero usuarios/permisos y equipos.

### Actas

Es importante, pero depende de usuarios, equipos, patrimonio/componentes y reglas de
documentacion.

### Dashboard completo

El dashboard viejo es visible y tentador, pero conviene construirlo despues de tener datos
limpios de equipos. Primero datos, despues resumen visual.

## Orden sugerido

```text
Usuarios y permisos minimos
  -> Administracion visual de usuarios/roles/modulos
  -> Equipos/API de inventario
  -> Dashboard simple
  -> Tareas tecnicas
  -> Stock/componentes
  -> Actas/reportes
```

