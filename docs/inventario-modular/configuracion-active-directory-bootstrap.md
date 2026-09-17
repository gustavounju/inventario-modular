# Configuracion Active Directory desde el sistema

## Objetivo

El sistema permite arrancar con `admin.local`, configurar Active Directory desde
`Configuracion tecnica`, probar la conexion LDAP y guardar los datos en MySQL. Despues de
guardar una configuracion valida, el siguiente ingreso debe hacerse contra Active
Directory y `admin.local` deja de servir como acceso normal.

## Valores por defecto

La pantalla `/admin/sistema/ad` queda precargada con los valores no secretos encontrados
en el inventario anterior:

```text
URL LDAP: ldap://10.15.0.41:389
Dominio: podjudsp.local
Base DN: DC=podjudsp,DC=local
Base de busqueda de usuarios: OU=USUARIOS,OU=PODJUDSP
Atributo nombre visible: displayName
Atributo fuero: department
Filtro de usuarios: (&(objectClass=user)(!(objectClass=computer)))
Limite de busqueda: 50
Usuario lector sugerido: gmurad
```

La clave del usuario lector nunca queda en codigo ni en documentacion. Debe cargarse en la
pantalla cuando se prueba la conexion.

## Flujo operativo

1. Iniciar el sistema en modo local con MySQL, sin H2.
2. Ingresar con `admin.local` y la clave bootstrap indicada al arrancar.
3. Abrir `Configuracion tecnica -> Active Directory`.
4. Completar o revisar URL, dominio, base DN, usuario lector y clave.
5. Presionar `Probar conexion`.
6. Si la prueba responde correctamente, presionar `Guardar configuracion`.
7. Cerrar sesion e ingresar nuevamente con usuario y clave de Active Directory.

Al guardar, Inventario Modular autoriza automaticamente como administrador al usuario
lector probado. Esto evita quedar afuera justo despues de deshabilitar el acceso bootstrap.

## Como se protegen las claves

La configuracion se guarda en la tabla `sistema_configuracion`. La clave LDAP se cifra con
AES/GCM mediante `SecretProtector`.

La clave de cifrado sale de:

```text
inventario.config-secret
INVENTARIO_CONFIG_SECRET
```

Si no se configura ese secreto, en desarrollo se crea `.local-secrets/inventario-config.key`.
Ese archivo esta ignorado por git y no debe subirse. En produccion conviene usar siempre
`INVENTARIO_CONFIG_SECRET`, guardarlo fuera del repositorio y conservarlo en backup seguro.

Si se pierde la clave de cifrado, el sistema no puede descifrar la clave LDAP guardada. En
ese caso hay que restaurar el secreto original o volver a cargar la configuracion.

## Que pasa si cambia la clave del lector

El login de cada persona contra Active Directory usa la clave que esa persona escribe en
la pantalla de ingreso. Por eso, si cambia la clave del usuario lector, los usuarios AD ya
autorizados pueden seguir entrando mientras el dominio valide sus propias claves.

Lo que puede fallar con la clave vieja del lector es la busqueda administrativa de usuarios
del dominio, la prueba de conexion y cualquier consulta LDAP de lectura. El administrador
debe entrar con una cuenta AD autorizada, abrir `Configuracion tecnica -> Active Directory`,
cargar la nueva clave del lector, probar y guardar.

Si justo no hay ningun administrador AD autorizado disponible, el rescate operativo es
corregir la configuracion en base de datos o restaurar temporalmente el acceso bootstrap en
un entorno controlado. Esa situacion debe tratarse como recuperacion administrativa, no como
flujo normal.

## Regla de base de datos

Para este flujo no se usa H2. En casa y en la PC del trabajo el perfil local trabaja contra
MySQL local. En produccion o en la red laboral, el mismo sistema puede apuntar a la base
remota que corresponda, pero la configuracion de Active Directory queda persistida en MySQL.

## Archivos principales

```text
src/main/java/ar/gov/justiciajujuy/sanpedro/inventario/web/SistemaConfigController.java
src/main/java/ar/gov/justiciajujuy/sanpedro/inventario/configuracion/LdapConfigurationService.java
src/main/java/ar/gov/justiciajujuy/sanpedro/inventario/configuracion/SecretProtector.java
src/main/java/ar/gov/justiciajujuy/sanpedro/inventario/security/DatabaseActiveDirectoryAuthenticationProvider.java
src/main/resources/templates/admin/sistema-ad.html
src/main/resources/db/migration/V26__configuracion_ldap_bootstrap.sql
```
