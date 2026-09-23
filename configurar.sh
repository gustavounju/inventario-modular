#!/bin/bash

echo "======================================================="
echo "    Configuracion Inicial de Base de Datos"
echo "======================================================="
echo ""
echo "Este script configurara sus credenciales de base de datos"
echo "localmente sin subirlas al repositorio git."
echo ""

read -p "Ingrese la URL de la base de datos (Ej. jdbc:mysql://127.0.0.1:3306/inventario_modular): " DB_URL
if [ -z "$DB_URL" ]; then
    DB_URL="jdbc:mysql://127.0.0.1:3306/inventario_modular"
fi

read -p "Ingrese el Usuario de la base de datos (Ej. root): " DB_USER
if [ -z "$DB_USER" ]; then
    DB_USER="root"
fi

read -s -p "Ingrese la Contrasena de la base de datos: " DB_PASS
echo ""

echo ""
echo "Guardando credenciales en application-secrets.properties..."

echo "# Archivo auto-generado por configurar.sh" > application-secrets.properties
echo "INVENTARIO_DB_URL=$DB_URL" >> application-secrets.properties
echo "INVENTARIO_DB_USER=$DB_USER" >> application-secrets.properties
echo "INVENTARIO_DB_PASSWORD=$DB_PASS" >> application-secrets.properties

echo ""
echo "[EXITO] application-secrets.properties creado correctamente."
echo "Ya puede arrancar el sistema con: ./mvnw spring-boot:run"
echo ""
