# Despliegue Ubuntu y publicacion de APK

Guia operativa para actualizar Inventario Modular en Ubuntu Server via PuTTY y publicar
la APK Android para los tecnicos desde el mismo servidor.

## Criterio general

El proyecto se divide en dos entregables:

- **Servidor web/backend**: se actualiza en Ubuntu desde GitLab con `git pull`, Maven y
  reinicio de `inventario-modular.service`.
- **APK Android**: se genera primero en Windows, se firma con la clave correspondiente y
  luego se copia al Ubuntu Server para que Inventario Modular la publique como descarga
  autenticada.

Por ahora no es necesario compilar Android en Ubuntu. Ubuntu solo ejecuta el JAR de
Spring Boot y sirve la APK ya generada.

## 1. Actualizar codigo del servidor via PuTTY

Entrar por PuTTY al servidor Ubuntu y confirmar que se esta en el servidor correcto:

```bash
hostname
cd /opt/inventario-modular
pwd
systemctl status inventario-modular.service --no-pager -l
```

Confirmar rama y estado del repositorio:

```bash
git remote -v
git branch --show-current
git status --short
```

La rama esperada es:

```text
primeros-pasos
```

Si `git status --short` muestra cambios locales no esperados, detenerse y revisarlos
antes de actualizar. No ejecutar comandos que borren cambios sin entenderlos.

Bajar lo ultimo desde GitLab:

```bash
git fetch origin
git checkout primeros-pasos
git pull --ff-only origin primeros-pasos
```

Validar y construir:

```bash
sh ./mvnw --batch-mode test
sh ./mvnw --batch-mode -DskipTests package
```

La salida esperada debe terminar en `BUILD SUCCESS`.

Reiniciar solamente el servicio nuevo:

```bash
sudo systemctl restart inventario-modular.service
systemctl status inventario-modular.service --no-pager -l
sudo journalctl -u inventario-modular.service -n 120 --no-pager
```

Este procedimiento no toca el inventario viejo ni `inventario.service`.

## 2. Verificar el servidor despues de actualizar

Desde el mismo Ubuntu:

```bash
curl -I http://127.0.0.1:8081/movil/login
curl -s http://127.0.0.1:8081/api/v1/sistema/estado
```

Desde una PC o celular de la red:

```text
http://IP_DEL_SERVIDOR:8081/movil/login
```

Ejemplo de laboratorio:

```text
http://192.168.1.8:8081/movil/login
```

Si desde Ubuntu responde pero desde el celular no, revisar firewall, Wi-Fi, VLAN o reglas
de red. Si responde `403`, revisar permisos del usuario y politica LAN-only.

## 3. Generar APK en Windows

La APK se genera desde Windows porque alli esta preparado el entorno Android.

Desde PowerShell:

```powershell
cd C:\Users\Gustavo\Documents\ChatGPT\Inventario-Modular\android
.\gradlew.bat :app:assembleLanRelease
```

La salida esperada es:

```text
C:\Users\Gustavo\Documents\ChatGPT\Inventario-Modular\android\app\build\outputs\apk\lanRelease\app-lanRelease.apk
```

Para cada nueva version hay que incrementar `versionCode` en:

```text
android/app/build.gradle
```

`versionName` identifica la version visible para el tecnico. La variante `lanRelease`
agrega el sufijo `-lan`.

## 4. Copiar APK al Ubuntu

Ruta estable recomendada en Ubuntu:

```text
/opt/inventario-modular/distribucion/tareas-lan.apk
```

Crear carpeta si no existe:

```bash
sudo mkdir -p /opt/inventario-modular/distribucion
```

Con WinSCP, copiar `app-lanRelease.apk` al servidor. Si no permite escribir directo en
`/opt`, copiar primero al home del usuario:

```text
/home/TU_USUARIO/tareas-lan.apk
```

Luego moverla a la ruta definitiva:

```bash
sudo cp /home/TU_USUARIO/tareas-lan.apk /opt/inventario-modular/distribucion/tareas-lan.apk
```

No subir la APK a GitLab como binario operativo. GitLab guarda el codigo y la
documentacion; la APK distribuida se publica desde la carpeta estable del servidor.

## 5. Permisos de archivo para descarga

El archivo debe ser legible por el usuario que ejecuta `inventario-modular.service`.

Ver usuario/grupo del servicio:

```bash
sudo systemctl show inventario-modular.service -p User -p Group
```

Permisos recomendados para la carpeta y APK:

```bash
sudo chown root:root /opt/inventario-modular/distribucion/tareas-lan.apk
sudo chmod 644 /opt/inventario-modular/distribucion/tareas-lan.apk
sudo chmod 755 /opt/inventario-modular/distribucion
```

`644` permite que el servicio lea el archivo. La descarga igualmente queda protegida por
login y permisos de Inventario Modular; no se publica anonimamente.

## 6. Configurar ruta de APK en el servidor

Editar el archivo de entorno:

```bash
sudo nano /etc/inventario-modular/inventario-modular.env
```

Agregar o corregir:

```env
INVENTARIO_MOVIL_APK_PATH=/opt/inventario-modular/distribucion/tareas-lan.apk
```

Proteger el archivo de entorno:

```bash
sudo chown root:root /etc/inventario-modular/inventario-modular.env
sudo chmod 600 /etc/inventario-modular/inventario-modular.env
```

Reiniciar:

```bash
sudo systemctl restart inventario-modular.service
```

Verificar que el endpoint exista:

```bash
curl -I http://127.0.0.1:8081/api/v1/movil/apk
curl -s http://127.0.0.1:8081/api/v1/movil/apk/info
```

Si devuelve redireccion a login, `401` o `403` sin sesion, es normal: la descarga requiere
usuario autenticado con permiso de tareas.

## 7. Actualizar tecnicos desde el celular

Los tecnicos entran por navegador o por la APK a:

```text
http://IP_DEL_SERVIDOR:8081/movil/tareas
```

La APK tambien tiene **Ajustes**. Cuando la version publicada en el servidor es distinta
de la instalada, muestra opcion de actualizacion. El tecnico toca actualizar, Android
descarga la APK y luego pide confirmar la instalacion.

Android puede seguir mostrando avisos de instalacion desde origen desconocido o Play
Protect si la app se distribuye por descarga directa. La firma release permite que la
misma app se actualice sobre la instalacion anterior, pero no elimina por si sola todos
los pasos de seguridad del sistema operativo.

## 8. Checklist por cada entrega

Servidor:

```bash
cd /opt/inventario-modular
git status --short
git fetch origin
git pull --ff-only origin primeros-pasos
sh ./mvnw --batch-mode test
sh ./mvnw --batch-mode -DskipTests package
sudo systemctl restart inventario-modular.service
curl -I http://127.0.0.1:8081/movil/login
```

APK:

```powershell
cd C:\Users\Gustavo\Documents\ChatGPT\Inventario-Modular\android
.\gradlew.bat :app:assembleLanRelease
```

Copiar al servidor:

```bash
sudo cp /home/TU_USUARIO/tareas-lan.apk /opt/inventario-modular/distribucion/tareas-lan.apk
sudo chmod 644 /opt/inventario-modular/distribucion/tareas-lan.apk
sudo systemctl restart inventario-modular.service
```

Verificacion desde celular:

- Abrir `/movil/login`.
- Ingresar con usuario tecnico autorizado.
- Abrir Ajustes.
- Confirmar version instalada y version publicada.
- Probar descarga/actualizacion.
- Activar avisos y probar sonido.
- Crear una tarea desde otra cuenta y confirmar que llegue el aviso.

## 9. Resumen para otra IA o desarrollador

Estado actual:

- Backend Spring Boot en rama `primeros-pasos`.
- Ubuntu esperado: `/opt/inventario-modular`.
- Servicio esperado: `inventario-modular.service`.
- Configuracion real fuera de Git: `/etc/inventario-modular/inventario-modular.env`.
- APK publicada por variable: `INVENTARIO_MOVIL_APK_PATH`.
- Ruta recomendada de APK: `/opt/inventario-modular/distribucion/tareas-lan.apk`.
- La APK es WebView LAN; consume `/movil/tareas`, `/api/v1/movil/avisos` y
  `/api/v1/movil/apk/info`.
- Las credenciales reales de MySQL, LDAP, tokens y firma Android no se versionan.

Regla de continuidad:

```text
GitLab conserva codigo y documentacion.
Windows genera la APK.
Ubuntu ejecuta el servidor y publica la APK generada.
```
