# Usuarios Locales y Active Directory

## Objetivo

Inventario Modular permite trabajar con dos tipos de identidad:

```text
AD     -> usuario validado por Active Directory.
LOCAL  -> usuario creado dentro de Inventario Modular con clave propia.
```

Ambos tipos pueden recibir roles, permisos y modulos. La diferencia esta en quien valida la
clave.

## Usuario de Active Directory

Un usuario `AD` es una cuenta existente en el dominio del Poder Judicial.

Flujo:

```text
Usuario ingresa usuario y clave
-> Active Directory valida la clave
-> Inventario Modular consulta MySQL
-> MySQL decide roles, permisos y modulos
```

Reglas:

- Inventario Modular no guarda claves de dominio.
- Inventario Modular no cambia claves de dominio.
- La pantalla de usuarios solo autoriza la identidad y le asigna permisos.
- Si la cuenta existe en AD pero no esta autorizada en MySQL, queda sin modulos.

## Usuario local

Un usuario `LOCAL` es una cuenta creada dentro de Inventario Modular.

Sirve para:

- trabajar desde casa sin Active Directory;
- crear usuarios temporales para tareas puntuales;
- dar acceso controlado a personas que no usan cuenta de dominio todos los dias;
- probar roles y permisos sin depender del servidor AD.

Ejemplo real:

```text
Un chofer no usa computadora todos los dias.
Durante horas sin viajes, se le asigna cargar inventario.
Se crea un usuario local temporal.
Se le asigna un rol limitado.
Cuando termina la tarea, se desactiva o elimina su acceso.
```

## Como se guarda la clave local

La clave local no se guarda en texto plano.

Inventario Modular guarda:

```text
credenciales_locales.password_hash
```

Ese valor es un hash BCrypt. Sirve para comparar la clave ingresada en el login, pero no
permite leer la clave original.

## Tablas involucradas

```text
usuarios
-> identidad autorizada
-> origen: AD o LOCAL
-> activo

credenciales_locales
-> solo para usuarios LOCAL
-> password_hash
-> requiere_cambio_clave

usuario_roles
-> roles asignados a cada identidad

rol_modulo_permisos
-> modulos y permisos que recibe cada rol
```

## Uso desde la pantalla

Abrir:

```text
/admin/usuarios
```

Para una cuenta de dominio:

```text
Origen de autenticacion: Active Directory
Clave local: dejar vacio
```

Para una cuenta local:

```text
Origen de autenticacion: Usuario local
Clave local: cargar una clave temporal de al menos 8 caracteres
```

## Configuracion

El proveedor de usuarios locales de base se controla con:

```properties
inventario.local-db-auth.enabled=true
```

En Windows/casa queda habilitado por defecto en los perfiles `local` y `casa`.

En produccion debe habilitarse con una variable de entorno si la institucion decide
permitir usuarios locales:

```bash
INVENTARIO_LOCAL_DB_AUTH_ENABLED=true
```

## Recomendacion operativa

Para usuarios locales temporales:

1. Crear el usuario con origen `LOCAL`.
2. Asignar el rol minimo necesario.
3. Usar una clave temporal.
4. Desactivar el usuario cuando termina la tarea.
5. No reutilizar claves entre personas.

## Estado actual

Implementado:

- origen `AD` o `LOCAL` en usuarios;
- tabla `credenciales_locales`;
- alta de usuario local con password desde API y pantalla;
- autenticacion de usuario local contra hash BCrypt;
- pruebas automatizadas para verificar que no se guarde password plano.

Pendiente:

- cambio de clave desde pantalla;
- activar/desactivar usuario desde pantalla;
- eliminar credencial local o desactivar usuario temporal;
- auditoria de altas/cambios de clave;
- busqueda/listado de usuarios de Active Directory.

