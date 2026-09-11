# Modulo movil y avisos internos

Primera version: 10 de septiembre de 2026.

## Accesos

- Modulo independiente: `/movil/tareas`.
- Ingreso local/Active Directory: `/movil/login`.
- Descarga autenticada de Android: `/api/v1/movil/apk`, disponible cuando existe el archivo configurado.
- Metadata autenticada de Android: `/api/v1/movil/apk/info`, usada por diagnostico y actualizaciones.
- En el Panel General hay un acceso **Tareas en el celular** junto al visor.

El criterio queda separado en dos caminos:

- Navegador del celular: portal de ingreso, prueba y descarga de la APK.
- APK instalada: modo operativo del tecnico. Abre directo `/movil/tareas`, reutiliza la
  sesion del WebView y redirige cualquier caida accidental en `/admin` hacia el modulo movil.

Cuando el servidor detecta el User-Agent `InventarioLAN/1`, oculta la tarjeta de descarga
de APK y la huella SHA-256. Ese dato queda reservado para diagnostico de distribucion.

El visor de administracion actualiza fichas y contadores cada 30 segundos con **En vivo**.
Conserva los filtros de la direccion abierta y pausa la actualizacion al editar un
formulario, para no perder lo escrito. Guardar o volver a abrir el visor reanuda el seguimiento.

El movil usa las operaciones y autorizaciones existentes de `/api/v1/tareas-tecnicas`.
Permite crear, buscar por tarea/usuario/equipo, tomar, editar, comentar, finalizar,
cancelar y eliminar segun los permisos existentes. Las tareas creadas por un tecnico
quedan a su cargo; un administrador puede dejarlas libres para que alguien las tome.
El estado visible de una tarea abierta es Pendiente, aunque internamente se conserva
EN_PROCESO para las tareas tomadas. Finalizadas y canceladas se distinguen.

El listado movil muestra una vista previa de hasta dos comentarios por tarea. La carga se
hace despues de pintar la lista para que la pantalla siga respondiendo rapido en celulares
con Wi-Fi institucional irregular.

El campo **Usuario solicitante** consulta Active Directory desde el servidor mediante
`/api/v1/movil/usuarios-dominio?q=...`. Al elegir un usuario se completan usuario, nombre
visible y fuero/oficina. Si LDAP no esta disponible o faltan credenciales lectoras, el
formulario conserva carga manual.

## Servidor Linux

Se utiliza el mismo proceso Spring Boot y la misma base MySQL. No se necesita otro
servidor de mensajes. Los nuevos endpoints respetan la autenticacion, los permisos
TAREAS/VER y el filtro LAN existente. Los cambios de tareas conservan proteccion CSRF.

La migracion `V15__avisos_tareas_lan.sql` agrega `tareas_avisos` y una secuencia bloqueada
durante el guardado. Esa secuencia evita que dos transacciones concurrentes produzcan
avisos confirmados fuera del orden del cursor. No hay FK desde el aviso a la tarea:
el historial puede sobrevivir a una eliminacion, que el cliente presenta como tarea
ya no disponible. Los avisos no tienen purga automatica en esta version.

Los perfiles `casa` y `local` ejecutan este SQL idempotente al arrancar, ya que actualmente
tienen Flyway desactivado. Para otro perfil, ejecutar V15 mediante el mecanismo de
migracion de ese entorno antes de habilitar esta version. No se modificaron datos del
servidor del trabajo durante el desarrollo local.

La migracion `V16__permisos_tecnico_tareas_movil.sql` otorga a `TECNICO` los permisos
`TAREAS/VER` y `TAREAS/EDITAR`. Sin esa asignacion, un tecnico puede autenticarse pero
recibe 403 al abrir la APK o consultar los avisos.

La migracion `V17__avisos_comentarios_destinatarios.sql` agrega tipo de aviso y
destinatario opcional. Una creacion de tarea queda como aviso general. Un comentario
en tarea sin responsable queda como aviso general; un comentario en tarea tomada queda
dirigido al responsable.

Ejemplo de variable en la unidad systemd existente, usando la ruta real del archivo:

```ini
Environment="INVENTARIO_MOVIL_APK_PATH=/opt/inventario-modular/distribucion/tareas-lan.apk"
```

El usuario del servicio debe poder leer la APK. No es necesario incluirla dentro del JAR.
Usar HTTPS con un nombre DNS interno y certificado de la CA institucional para el
despliegue de produccion. Android admite la CA institucional instalada en el almacen
de certificados del dispositivo. La red debe permitir el trafico entre clientes Wi-Fi
y servidor; el aislamiento entre clientes del punto de acceso puede impedirlo.

## API de avisos

`GET /api/v1/movil/sesion` devuelve usuario actual y permisos de operacion.

`GET /api/v1/movil/usuarios-dominio?q=texto` devuelve candidatos de Active Directory
para autocompletar solicitantes. Requiere sesion autenticada y permiso `TAREAS/VER`;
la respuesta marca `disponible=false` si LDAP esta deshabilitado o no se pudo consultar.

`GET /api/v1/movil/avisos` inicia el seguimiento en el ultimo cursor, sin historial.

`GET /api/v1/movil/avisos?despuesDe=123` devuelve hasta 100 avisos posteriores y
`siguiente`. El cliente guarda el cursor luego de procesar el lote. La consulta puede
repetirse tras una desconexion; el servidor no elimina avisos al consultarlos.

`GET /api/v1/movil/apk/info` devuelve disponibilidad, nombre, tamano, fecha y SHA-256
de la APK publicada, junto con `version`. Requiere usuario autorizado, pero no permiso
`TAREAS/VER`, para permitir diagnosticar una instalacion antes de resolver permisos del
modulo.

Los avisos cubren creacion de tareas y comentarios. La creacion y los comentarios en
tareas sin responsable se anuncian a todos los tecnicos habilitados; los comentarios
en tareas tomadas se dirigen al responsable. No hay aun seleccion de destinatarios por
sede ni confirmacion de lectura por tecnico. Las operaciones posteriores de la tarea se
siguen consultando por la API existente.

Desde la APK Android se habilita un boton **Dictar** para abrir el reconocimiento de
voz del telefono y precargar una nueva tarea. El texto se envia a
`POST /api/v1/movil/dictado/interpretar`: si `INVENTARIO_IA_OPENAI_ENABLED=true` y el
servidor tiene `OPENAI_API_KEY`, se consulta IA real desde el backend; si no, se usa una
extraccion local basica. La APK nunca guarda claves de IA.

## Instalacion Android y prueba de campo

Ver [guia de la APK](../../android/README.md). La web funciona en el navegador;
los avisos de fondo requieren activar el servicio Android y configurar el telefono.

La variante LAN firmada `0.1.7-lan` queda preconfigurada con `http://192.168.1.8:8081`.
En produccion la direccion debe venir de configuracion institucional; HTTPS sigue siendo
el objetivo recomendado cuando exista dominio o certificado institucional.

El servicio Android de avisos:

- Consulta `/api/v1/movil/avisos` durante la jornada activada por el tecnico.
- Mantiene `PARTIAL_WAKE_LOCK` y `WifiLock` mientras los avisos estan activos.
- Usa el canal `tareas-nuevas-v3` con tono propio `tareas_lan_alert.wav`.
- Solicita quitar la app de optimizacion de bateria para mejorar entrega con pantalla bloqueada.

Android puede demorar notificaciones si el fabricante restringe segundo plano, inicio
automatico, Wi-Fi en reposo, No molestar o el canal de notificaciones. En esos casos hay
que ajustar el telefono aunque el codigo del servicio este activo. No se validaron iOS
ni todos los modos Doze/fabricante en esta PC.

Pruebas automatizadas del servidor:

```powershell
.\mvnw.cmd '-Dtest=TareaMovilControllerTests,TareaTecnicaControllerTests,TareaTecnicaPageControllerTests,LocalAuthenticationConfigTests' test
```

La prueba movil incluye ingreso local y destino seguro, permisos, CSRF, persistencia
de avisos, inicializacion idempotente, rollback, paginacion y toma concurrente. Active
Directory se reutiliza sin cambios, pero su conexion real debe probarse en el trabajo.

Prueba puntual posterior:

```powershell
.\mvnw.cmd -Dtest=TareaMovilControllerTests test
```

Resultado del 10 de septiembre de 2026: 7 pruebas, 0 fallos, 0 errores.
