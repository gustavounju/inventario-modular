# Firma Android de produccion

Primera version: 10 de septiembre de 2026.

## Criterio

La firma release identifica a la institucion que publica la APK. Android la usa para
permitir actualizaciones futuras de la misma app y para distinguirla de una APK debug.
No convierte por si sola una descarga directa en instalacion silenciosa: si la APK se
instala fuera de Google Play o de un MDM, Android puede seguir mostrando avisos de
origen desconocido o Play Protect.

La clave privada no debe subirse a GitLab, enviarse por chat ni quedar dentro del JAR.
Si se pierde la clave, no se podran publicar actualizaciones sobre la misma instalacion
release; habra que desinstalar y volver a instalar con otra firma.

## Archivos locales

En esta estacion la configuracion esperada por Gradle queda fuera de Git:

```text
.local-secrets/android/release-signing.properties
.local-secrets/android/tecnico-taller-san-pedro-release.p12
```

El archivo `release-signing.properties` debe contener:

```properties
storeFile=C:/ruta/segura/tecnico-taller-san-pedro-release.p12
storeType=pkcs12
storePassword=...
keyAlias=tecnico-taller-san-pedro
keyPassword=...
```

El repositorio ignora `.local-secrets/`, `*.p12` y `*.jks`. Hacer respaldo offline de ambos
archivos en un medio institucional protegido.

## Compilar

La variante `debug` sigue siendo piloto y acepta HTTP LAN. La variante `lanRelease`
queda firmada con clave institucional, no agrega sufijo al paquete y acepta HTTP LAN
para pruebas de campo por IP, incluyendo un `network_security_config` propio que permite
cleartext. La variante `release` queda firmada si existe el archivo local de secretos,
pero mantiene `cleartext=false`; para uso real requiere HTTPS confiable.

Desde `android/`:

```powershell
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :app:assembleLanRelease
```

Salida esperada:

```text
android/app/build/outputs/apk/release/app-release.apk
android/app/build/outputs/apk/lanRelease/app-lanRelease.apk
```

Verificacion:

```powershell
$apksigner = "C:\ruta\android-sdk\build-tools\35.0.0\apksigner.bat"
& $apksigner verify --verbose --print-certs android\app\build\outputs\apk\release\app-release.apk
```

Registrar versionCode, versionName, SHA-256 de la APK y huella SHA-256 del certificado.

## Distribucion

Opciones recomendadas, de mayor a menor confianza operativa:

- Managed Google Play con administracion institucional de dispositivos.
- MDM institucional que instale y actualice APKs firmadas internamente.
- Portal interno HTTPS con descarga autenticada.
- Portal HTTP por IP solo para laboratorio o red aislada, no para claves reales.

Sin Play/MDM, Android seguira pidiendo aceptar instalacion desde origen desconocido,
aunque la APK este firmada. Con Play/MDM, el usuario final recibe menos pasos y la
instalacion queda gobernada por politicas institucionales.
