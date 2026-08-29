# Incidente: Login Local Repetido

## Resumen

En Windows/casa, el login local simulado podia aceptar correctamente el primer ingreso y
luego rechazar intentos siguientes con:

```text
Invalid credentials
```

El problema no estaba en MySQL, ni en la tabla de usuarios, ni en Active Directory. La
causa estaba en como se construia el usuario local en memoria para Spring Security.

## Entorno afectado

- Sistema: Inventario Modular Java.
- Entorno: Windows local/casa.
- Perfil: `local` o `casa`.
- Dominio: apagado con `inventario.ldap.enabled=false`.
- Usuario local: `admin.local`.
- Clave local actual: `AdminLocal123`.

Produccion/trabajo con Active Directory real no se modifico por este arreglo.

## Sintoma

El usuario intentaba entrar desde:

```text
http://192.168.1.8:8081/login
```

con:

```text
Usuario: admin.local
Clave: AdminLocal123
```

y la pantalla respondia:

```text
Invalid credentials
```

Durante la prueba tecnica se observo que el login podia funcionar una vez y fallar en el
siguiente intento, aunque la clave fuera correcta.

## Causa tecnica

La clase `LocalAuthenticationConfig` creaba una sola instancia de
`ActiveDirectoryUserDetails` y la devolvia en cada busqueda del usuario local.

Spring Security, por seguridad, borra las credenciales del objeto `UserDetails` despues de
una autenticacion correcta. Como el mismo objeto se reutilizaba, el primer login podia
dejar la clave borrada dentro de esa instancia. El segundo intento comparaba la clave
ingresada contra un usuario en memoria que ya no tenia credenciales disponibles.

Resultado:

```text
Primer login: correcto.
Segundo login: Invalid credentials.
```

## Solucion aplicada

Se modifico `LocalAuthenticationConfig` para conservar la clave codificada en una variable
interna y devolver una instancia nueva de `ActiveDirectoryUserDetails` en cada busqueda.

Archivo principal:

```text
src/main/java/ar/gov/justiciajujuy/sanpedro/inventario/config/LocalAuthenticationConfig.java
```

Tambien se simplifico la clave local por defecto para evitar problemas de copiado en
PowerShell:

```text
Antes: AdminLocal123!
Ahora: AdminLocal123
```

Archivos de configuracion afectados:

```text
src/main/resources/application-local.properties
src/main/resources/application-casa.properties
src/main/java/ar/gov/justiciajujuy/sanpedro/inventario/config/LocalAuthenticationProperties.java
```

## Prueba agregada

Se agrego una prueba de regresion que realiza dos logins seguidos con el mismo usuario y la
misma clave. Antes del arreglo, el segundo login fallaba. Despues del arreglo, ambos
ingresos redirigen correctamente a `/admin`.

Archivo de prueba:

```text
src/test/java/ar/gov/justiciajujuy/sanpedro/inventario/security/LocalAuthenticationConfigTests.java
```

Prueba:

```text
allowsRepeatedLocalLoginsWithSameCredentials
```

## Verificacion realizada

Se ejecuto:

```powershell
mvn -Dtest=LocalAuthenticationConfigTests test
mvn test
```

Resultado:

```text
BUILD SUCCESS
19 tests, 0 failures, 0 errors
```

Tambien se verifico el login real del servidor local por IP:

```text
http://192.168.1.8:8081/login
```

Resultado:

```text
Intento 1 -> /admin
Intento 2 -> /admin
```

## Commit

El arreglo quedo registrado en Git con:

```text
d810116 fix: corrige login local repetido
```

Ese commit fue subido a GitLab en la rama:

```text
primeros-pasos
```

GitHub queda como copia de seguridad manual para practicar versionado:

```powershell
git push github primeros-pasos
```

## Leccion tecnica

En Spring Security, no conviene reutilizar una misma instancia mutable de `UserDetails`
cuando el framework puede borrar credenciales despues de autenticar. Para usuarios
generados en memoria desde configuracion, es mas seguro devolver una instancia nueva por
cada busqueda.

## Estado

Resuelto.

