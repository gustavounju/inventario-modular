@echo off
setlocal enabledelayedexpansion

echo =======================================================
echo     Configuracion Inicial de Base de Datos
echo =======================================================
echo.
echo Este script configurara sus credenciales de base de datos
echo localmente sin subirlas al repositorio git.
echo.

set /p DB_URL="Ingrese la URL de la base de datos (Ej. jdbc:mysql://127.0.0.1:3306/inventario_modular): "
if "!DB_URL!"=="" set DB_URL=jdbc:mysql://127.0.0.1:3306/inventario_modular

set /p DB_USER="Ingrese el Usuario de la base de datos (Ej. root): "
if "!DB_USER!"=="" set DB_USER=root

set /p DB_PASS="Ingrese la Contrasena de la base de datos: "

echo.
echo Guardando credenciales en application-secrets.properties...

echo # Archivo auto-generado por configurar.bat > application-secrets.properties
echo INVENTARIO_DB_URL=!DB_URL!>> application-secrets.properties
echo INVENTARIO_DB_USER=!DB_USER!>> application-secrets.properties
echo INVENTARIO_DB_PASSWORD=!DB_PASS!>> application-secrets.properties

echo.
echo [EXITO] application-secrets.properties creado correctamente.
echo Ya puede arrancar el sistema con: .\mvnw.cmd spring-boot:run
echo.
pause
