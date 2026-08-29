# Seguridad Modular Inicial

Este documento explica la primera capa de autorizacion local de Inventario Modular.

## Idea principal

El sistema separa dos responsabilidades:

```text
Active Directory valida identidad.
Inventario Modular decide autorizacion, roles, permisos y modulos.
```

Esto permite que un usuario use su cuenta real de dominio en el trabajo, pero que el
sistema Java decida si ese usuario puede ver `EQUIPOS`, `MUEBLES`, `PATRIMONIO`,
`COMPONENTES`, `USUARIOS`, `REPORTES` u otros modulos.

En casa, donde no hay dominio real, el perfil local simula el login con `admin.local`.
Esa simulacion no reemplaza Active Directory; solo permite desarrollar y estudiar desde
Windows.

## Migracion de base de datos

La primera migracion Flyway esta en:

```text
src/main/resources/db/migration/V1__seguridad_modular.sql
```

Crea estas tablas:

```text
usuarios
roles
modulos
permisos
usuario_roles
rol_modulo_permisos
```

La relacion queda asi:

```text
usuario -> roles -> modulos -> permisos
```

Ejemplo:

```text
admin.local
  -> ADMINISTRADOR
     -> todos los modulos
        -> VER, CREAR, EDITAR, ELIMINAR, EXPORTAR, ADMINISTRAR
```

## Modulos iniciales

La migracion deja cargados estos modulos base:

- `EQUIPOS`
- `ACTAS`
- `MUEBLES`
- `PATRIMONIO`
- `STOCK`
- `COMPONENTES`
- `USUARIOS`
- `REPORTES`
- `TAREAS`

Estos nombres son el punto de partida. Se pueden apagar o prender por permisos asignados
a roles y usuarios.

## Endpoints API

Se agregaron endpoints pensados para API-first y futura app movil:

```text
GET /api/v1/me
GET /api/v1/me/modulos
GET /api/v1/usuarios
POST /api/v1/usuarios
GET /api/v1/roles
```

`GET /api/v1/me` devuelve:

- usuario autenticado;
- nombre visible;
- fuero;
- si esta autorizado localmente;
- modulos habilitados;
- permisos por modulo.

`GET /api/v1/me/modulos` devuelve solo los modulos que el usuario puede ver.

Estos endpoints son importantes porque la futura app movil podra consumir la misma
informacion sin depender de la pantalla web.

### Administracion inicial de usuarios por API

Los endpoints de administracion requieren que el usuario autenticado tenga permiso
`ADMINISTRAR` sobre el modulo `USUARIOS`.

`GET /api/v1/usuarios` devuelve los usuarios cargados localmente en Inventario Modular:

```json
{
  "usuarios": [
    {
      "id": 1,
      "username": "admin.local",
      "nombreVisible": "Administrador Local",
      "fuero": "Desarrollo local",
      "activo": true,
      "roles": ["ADMINISTRADOR"]
    }
  ]
}
```

`POST /api/v1/usuarios` crea un usuario local autorizado. No recibe clave ni guarda clave
de dominio.

Ejemplo:

```json
{
  "username": "gmurad",
  "nombreVisible": "Gustavo Elias Murad",
  "fuero": "Informatica",
  "activo": true,
  "roles": ["ADMINISTRADOR"]
}
```

`GET /api/v1/roles` devuelve los roles disponibles para que una pantalla futura pueda
mostrarlos como opciones, sin tener codigos fijos escritos a mano.

Respuestas esperadas:

- `201 Created`: usuario creado correctamente.
- `403 Forbidden`: el usuario autenticado no puede administrar usuarios.
- `409 Conflict`: el usuario ya existe.
- `422 Unprocessable Content`: algun rol solicitado no existe.

## Pantalla administrativa actual

La pantalla `/admin` muestra:

- usuario autenticado;
- cuenta;
- fuero;
- estado `Autorizado` o `Pendiente de autorizacion`;
- modulos habilitados;
- permisos de cada modulo;
- atributos de identidad de AD o del modo local.

Todavia no existe una pantalla completa para crear usuarios, editar roles o asignar
permisos. Eso corresponde al siguiente paso del modulo `USUARIOS`.

## Comportamiento actual

Si el usuario existe en `usuarios` y esta activo:

```text
autorizado = true
```

El sistema busca sus roles y devuelve los modulos/permisos asociados.

Si el usuario inicia sesion correctamente por AD pero todavia no existe en `usuarios`:

```text
autorizado = false
```

El sistema mantiene la identidad visible, pero no devuelve modulos. Por ahora esto se usa
para no bloquear accidentalmente el acceso mientras se termina la administracion inicial
de usuarios.

## Lo que falta

Los siguientes pasos son:

- bloquear acceso a modulos cuando `autorizado = false`;
- crear pantalla administrativa para usuarios consumiendo `GET /api/v1/usuarios`,
  `POST /api/v1/usuarios` y `GET /api/v1/roles`;
- permitir editar roles de usuarios existentes;
- permitir activar/desactivar modulos;
- aplicar permisos en endpoints concretos, por ejemplo `EQUIPOS`;
- dejar tests de acceso permitido y denegado por modulo.

## Pruebas

Las pruebas relacionadas estan en:

```text
src/test/java/ar/gov/justiciajujuy/sanpedro/inventario/web/CurrentUserControllerTests.java
src/test/java/ar/gov/justiciajujuy/sanpedro/inventario/web/UsuarioAdminControllerTests.java
src/test/java/ar/gov/justiciajujuy/sanpedro/inventario/security/LocalAuthenticationConfigTests.java
src/test/resources/sql/seguridad-modular-test.sql
src/test/resources/sql/limpiar-seguridad-modular-test.sql
```

Comando de verificacion:

```powershell
mvn test
```

Salida esperada:

```text
BUILD SUCCESS
```
