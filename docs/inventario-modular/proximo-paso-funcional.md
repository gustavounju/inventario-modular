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

Hoy el sistema ya puede validar un usuario de AD, pero falta definir localmente si ese
usuario puede entrar, que rol tiene y que modulos puede ver.

## Proximo sprint recomendado

### Sprint 1: Usuarios y permisos minimos

Objetivo:

- Crear tabla de usuarios autorizados.
- Crear tabla de roles.
- Crear tabla de modulos.
- Crear permisos minimos por modulo.
- Permitir que solo usuarios autorizados localmente entren al panel.
- Dejar un usuario administrador inicial.

Resultado esperado:

- Usuario valido en AD pero no autorizado localmente: no entra.
- Usuario autorizado: entra y ve sus modulos.
- Administrador: puede ver todo.
- No se guardan claves de AD.

Este sprint es pequeno pero muy importante porque evita construir modulos encima de una
seguridad incompleta.

### Sprint 2: Equipos como primer modulo funcional

Despues de cerrar permisos, el primer modulo funcional recomendado es `Equipos`.

Motivo:

- Es el corazon del inventario viejo.
- Alimenta dashboard, tareas, reportes, mapas, stock asignado y actas.
- Ya existen reglas reales: nombre de PC, ultimo usuario, fuero, IP, sistema operativo,
  procesador, RAM, impresora, monitoreo y estado activo.
- Permite probar desde temprano el enfoque API-first.

Primer alcance de Equipos:

- Migracion Flyway para tabla `equipos`.
- Endpoint `GET /api/v1/equipos`.
- Endpoint `GET /api/v1/equipos/{id}`.
- Endpoint interno para recibir inventario de una PC.
- Pantalla simple de listado.
- Busqueda por nombre, usuario o fuero.
- Tests de repositorio/controlador.

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
  -> Equipos/API de inventario
  -> Dashboard simple
  -> Tareas tecnicas
  -> Stock/componentes
  -> Actas/reportes
```

