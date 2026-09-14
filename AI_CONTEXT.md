# Inventario Modular - Contexto Maestro para IA

Este documento es la entrada principal para cualquier IA, agente o desarrollador nuevo que
necesite entender Inventario Modular sin reconstruir el contexto desde conversaciones sueltas,
bitacoras largas o comentarios dispersos.

La regla de uso es simple: antes de modificar el sistema, leer este archivo completo y despues
consultar los documentos especificos enlazados al final.

## 1. Que es Inventario Modular

Inventario Modular es una aplicacion Java/Spring Boot para el Departamento de Informatica del
Centro Judicial San Pedro. Reemplaza y moderniza el inventario anterior, pero no copia su deuda
tecnica: toma sus flujos utiles como referencia y los reconstruye con modulos, permisos, API,
base de datos MySQL y pantallas web/moviles.

El sistema administra:

- Equipos informaticos.
- Componentes instalados y detectados por script.
- Stock fisico de componentes del taller.
- Tareas tecnicas y avisos LAN para tecnicos.
- APK Android LAN para operar desde celular.
- Usuarios, roles y permisos.
- Actas, movimientos, ubicaciones, muebles, bienes patrimoniales y reportes.

La idea funcional central es mantener un inventario fisico y un gemelo digital: lo que esta
cargado administrativamente debe poder compararse con lo detectado o instalado realmente.

La metodologia rectora queda definida como **ITAM/CMDB de taller**: `Equipo` es el activo,
los componentes administrativos son la configuracion esperada, lo reportado por script es la
configuracion detectada, `GemeloDigitalService` consolida la vista CMDB, las diferencias
generan decision/trabajo, `StockComponente` representa capacidad de reparacion,
`TareaTecnica` representa accion operativa y actas/auditoria/comentarios son evidencia.
Ver `docs/inventario-modular/metodologia-itam-cmdb-taller.md` y
`docs/decisions/ADR-009-metodologia-itam-cmdb-de-taller.md`.

## 2. Reglas de negocio clave

- La base principal es MySQL.
- El perfil local de trabajo usa `application-local.properties`.
- Active Directory puede autenticar identidad, pero Inventario Modular decide autorizacion,
  roles, permisos y modulos.
- Los usuarios locales existen para laboratorio, desarrollo y contingencia.
- El perfil `local` debe mantener `inventario.local-db-auth.enabled=true`; si queda en `false`,
  la APK no acepta usuarios tecnicos locales de la base como `usuario3`.
- Un usuario autenticado puede existir pero no ver nada si no tiene permisos.
- `usuario3` debe quedar como tecnico.
- La APK LAN es una WebView Android con funciones nativas para escaneo, avisos y actualizacion.
- La app movil consume las pantallas `/movil/tareas` y `/movil/stock`.
- En `/movil/tareas`, al crear una tarea, el solicitante no debe forzarse al usuario logueado:
  el tecnico puede buscar predictivamente usuarios de AD o escribir manualmente quien pidio
  ayuda. El responsable si puede seguir siendo el tecnico logueado cuando no es administrador.
- `solicitanteUsername` es el usuario AD del solicitante solo cuando se eligio desde AD. Si la
  carga fue manual puede coincidir con el nombre visible y no debe mostrarse como "usuario
  logueado".
- `responsable` es el tecnico del taller que opera la tarea. El fuero/oficina pertenece al
  solicitante, no al responsable.
- El scanner de la APK debe abrir en vertical.
- El tecnico puede escanear codigo de barra o serie desde el celular para sumar stock.
- Al cargar stock desde el celular, se debe guardar que usuario lo ingreso.
- El tecnico puede usar escaneo rapido para enviar muchos codigos de barra al stock sin completar
  datos tecnicos en el momento.
- Los codigos ingresados por escaneo rapido quedan como `TipoComponente.PENDIENTE` y con
  `datos_completos = false` hasta que un administrador complete el lote desde la web.
- Si un componente esta `DISPONIBLE` en Stock, se asume que esta fisicamente en el taller.
- Si un componente se toma desde una tarea tecnica, debe pasar de `DISPONIBLE` a `RESERVADO`.
  `ASIGNADO` queda reservado para salida real instalada/vinculada a un equipo, porque el
  sincronizador de stock huerfano libera piezas `ASIGNADO` sin componente activo asociado.
- En Stock no usar "deposito", "deposito taller" ni "ubicacion fisica" para piezas disponibles.
- Los campos obligatorios al ingresar stock desde celular son: codigo/serie, tipo de componente,
  modelo y capacidad cuando corresponda al caso operativo. El resto puede quedar para que el
  administrador lo complete desde la web.
- El administrador puede aplicar en lote el mismo remito, orden de compra o proveedor a varios
  componentes seleccionados.
- Una pieza de stock usada en una tarea debe quedar trazable.

## 3. Arquitectura tecnica

- Backend: Java 21, Spring Boot 4, Spring MVC, Spring Security, Spring Data JPA.
- Persistencia: MySQL en local/produccion; H2 solo para perfiles de casa/pruebas cuando aplique.
- Frontend web: Thymeleaf, CSS y JavaScript estatico.
- Movil: Android nativo simple en `android/`, WebView hacia el servidor LAN.
- Build backend: Maven Wrapper `mvnw.cmd`.
- Build APK: Gradle Wrapper en `android/gradlew.bat`.
- CI/CD: GitLab CI documentado en `docs/inventario-modular/ci-cd.md`.

### Arquitectura visual vigente

- El tema visual vigente del servidor web y la APK es **Institucional Justicia Jujuy**.
- El objetivo es una interfaz sobria, clara y administrativa para el **Centro Judicial San Pedro**
  y el **Taller Informatica**, aplicada de forma fiel al mockup aprobado
  `output/mockups-diseno/justicia-jujuy-inventario.html`.
- El archivo central de estilos es `src/main/resources/static/css/admin.css`.
- La navegacion compartida vive en `src/main/resources/templates/admin/nav.html`.
- La documentacion visual canonica esta en `docs/arquitectura_visual.md`.
- La cabecera superior usa azul `#006f99` y el texto serif italico
  `Poder Judicial de la Provincia de Jujuy`, como en el mockup.
- La paleta vigente es siempre clara; aunque exista `data-theme="dark"` por configuracion
  previa del navegador, debe resolver a los mismos colores del mockup.
- El sidebar no debe repetir `Centro Judicial San Pedro` ni `Taller Informatica`; empieza con
  `Sistema de inventario` y solo contiene navegacion operativa.
- Los colores base del tema institucional son:
  - azul institucional `#006f99`
  - azul profundo `#005777` / `#014760`
  - fondo celeste `#dceef7`
  - paneles `#f8fcfe`
  - bordes `#b8cdd7`
- Al tocar UI web o movil, preservar:
  - identidad judicial superior como en el mockup;
  - `Centro Judicial San Pedro` y `Taller Informatica` donde aporten contexto, sin duplicarlos
    en el panel lateral;
  - paneles compactos, claros y de esquinas cuadradas;
  - evitar sombras/fondos decorativos detras de textos sueltos;
  - normalizar cards, badges, banners, tablas y formularios heredados para que no conserven
    gradientes, radios grandes ni colores de temas anteriores;
  - foco visible en formularios y botones;
  - tablas densas pero legibles;
  - compatibilidad responsive con desktop y celular.
- Antes de cambiar la identidad visual, revisar primero el mockup de referencia:
  `output/mockups-diseno/justicia-jujuy-inventario.html`, si existe en el entorno local.

## 4. Directorios importantes

| Ruta | Contenido |
|---|---|
| `src/main/java/ar/gov/justiciajujuy/sanpedro/inventario` | Codigo Java principal |
| `src/main/java/.../web` | Controladores web y API |
| `src/main/java/.../security` | Autenticacion, autorizacion, filtros y usuarios |
| `src/main/java/.../equipos` | Inventario de equipos |
| `src/main/java/.../componentes` | Componentes instalados/detectados del equipo |
| `src/main/java/.../stock` | Stock de componentes |
| `src/main/java/.../tareas` | Tareas tecnicas, avisos y uso de stock |
| `src/main/resources/templates/admin` | Pantallas web administrativas |
| `src/main/resources/templates/movil` | Pantallas moviles usadas por la APK |
| `src/main/resources/static/js` | JavaScript web/movil |
| `src/main/resources/static/css` | Estilos |
| `src/main/resources/db/migration` | Migraciones SQL para MySQL |
| `src/main/resources/db/casa` | Esquema/datos H2 para perfil casa |
| `src/test` | Tests automatizados |
| `android` | Proyecto Android de la APK LAN |
| `docs/inventario-modular` | Documentacion operativa y funcional |
| `docs/decisions` | ADRs y decisiones arquitectonicas |
| `scripts` | Scripts de instalacion, despliegue y soporte |
| `output/android` | APK publicada/copias de salida |

## 5. Modulos funcionales

| Modulo | Para que sirve | Clases/pantallas clave |
|---|---|---|
| `EQUIPOS` | Alta, consulta y actualizacion de equipos | `EquipoPageController`, `EquipoController`, `admin/equipos.html` |
| `COMPONENTES` | Componentes esperados, detectados e instalados en equipos | `ComponenteService`, `ComponenteController`, `admin/equipo-detalle.html` |
| `STOCK` | Ingreso, reserva, asignacion y administracion de componentes disponibles | `StockService`, `StockController`, `StockPageController`, `admin/stock.html`, `movil/stock.html` |
| `TAREAS` | Gestion de tareas tecnicas, toma de tareas, comentarios y avisos | `TareaTecnicaService`, `TareaTecnicaController`, `TareaMovilController` |
| `ORDENES_ARMADO` | Armado/reserva de componentes para equipos | Servicios y vistas vinculadas a ordenes |
| `USUARIOS` | Administracion de usuarios, roles y permisos | `UsuarioAdminController`, `admin/usuarios.html` |
| `UBICACIONES` | Sedes, oficinas y ubicaciones generales del inventario | `UbicacionService`, `admin/ubicaciones.html` |
| `ACTAS` | Actas y documentacion oficial | `ActaService`, `ActaPageController`, `admin/actas.html` |
| `MUEBLES` | Muebles y mobiliario judicial | `MuebleService`, `admin/muebles.html` |
| `REPORTES` | Exportes, CSV y reportes | `ReporteService`, `ReporteController` |
| `AUDITORIA` | Registro de eventos y movimientos | `AuditoriaService`, controladores de auditoria |
| `APK LAN` | App Android para tecnicos | `android/app`, `MainActivity`, `PortraitCaptureActivity` |

### Mapa ITAM/CMDB vigente

| Capa | Implementacion actual |
|---|---|
| Activo | `Equipo`, `equipos`, `/admin/equipos` |
| Configuracion esperada | `Componente` cargado por admin/stock/orden |
| Configuracion detectada | Script Windows, `/api/v1/equipos/inventario` |
| Gemelo digital | `GemeloDigitalService`, dashboard de diferencias |
| Diferencia | Comparacion entre esperado y detectado |
| Stock | `StockComponente`, `/admin/stock`, `/movil/stock` |
| Trabajo tecnico | `TareaTecnica`, APK, visor publico |
| Evidencia | `Acta`, `AuditoriaService`, comentarios, stock usado |

### Modulo funcional: Stock de Componentes

Reglas actuales de Stock:

- La pantalla admin es `src/main/resources/templates/admin/stock.html`.
- La pantalla movil es `src/main/resources/templates/movil/stock.html`.
- La API principal es `/api/v1/stock/componentes`.
- La logica central vive en `StockService`.
- La entidad principal es `StockComponente`.
- La tabla principal es `stock_componentes`.
- `stock_componentes.ingresado_por` guarda el usuario que ingreso la pieza a stock.
- `stock_componentes.datos_completos` separa los componentes listos de los pendientes de completar.
- `TipoComponente.PENDIENTE` es un tipo temporal para codigos escaneados masivamente desde celular.
- En `/admin/stock`, los componentes con `datos_completos=false` van en la pestana
  "Pendientes de completar"; no deben mezclarse visualmente con "Componentes en Stock".
- `TipoComponente.CARTUCHO` existe para cartuchos de tinta/insumos similares.
- Si un componente esta disponible, esta en el taller. No mostrar ni pedir ubicacion fisica para
  el stock disponible.
- En el listado admin debe verse quien ingreso el componente, cuando exista ese dato.
- El alta movil debe permitir escanear codigo de barra/serie y completar tipo, modelo y capacidad.
- Remito, orden de compra y proveedor pueden cargarse luego desde admin.
- El admin debe poder seleccionar varios componentes y aplicarles el mismo remito, orden de compra
  o proveedor.
- El admin tambien debe poder completar en lote tipo, descripcion tecnica, marca, modelo, capacidad
  y observaciones para codigos cargados rapidamente desde celular.
- Cuando un pendiente se completa con un tipo real y una descripcion real, pasa automaticamente a
  la pestana de Stock disponible porque `datos_completos` queda en `true`.

Decision de UI vigente:

- La solapa **"Modificar Piezas Cargadas"** no debe existir como solapa separada.
- Cada registro de la tabla de Stock debe tener su propio boton de edicion.
- La columna **Tipo** de la tabla de Stock debe usar un chip estable y legible, no estilos de
  `step-badge` pensados para pasos del circuito.

## 6. Flujos clave

### Alta de stock desde celular

1. El tecnico entra a `/movil/stock` desde la APK.
2. Toca el icono de escaneo.
3. La APK abre el scanner nativo en vertical.
4. Al leer codigo de barra o serie, el valor vuelve al formulario movil.
5. El tecnico elige tipo de componente y completa modelo/capacidad si corresponde.
6. El formulario llama a `POST /api/v1/stock/componentes`.
7. `StockController` toma el usuario autenticado.
8. `StockService.crear` guarda el componente y registra `ingresado_por`.
9. El componente queda `DISPONIBLE`.

### Escaneo rapido de muchos codigos

1. El tecnico entra a `/movil/stock`.
2. Usa el panel "Escaneo rapido".
3. Escanea o escribe muchos codigos seguidos.
4. La pantalla arma una lista local, permite quitar errores y envia el lote.
5. El formulario llama a `POST /api/v1/stock/componentes/lote-rapido`.
6. `StockService.crearPendientesDesdeCodigos` crea un componente por codigo.
7. Cada componente queda con:
   - `tipo = PENDIENTE`
   - `estado = DISPONIBLE`
   - `serial = codigo escaneado`
   - `descripcion = Pendiente de completar - codigo`
   - `ingresado_por = usuario autenticado`
   - `datos_completos = false`
8. El administrador entra a `/admin/stock`, abre la pestana "Pendientes de completar",
   selecciona los codigos necesarios y aplica datos masivos.

### Edicion administrativa de varios componentes

1. El administrador entra a `/admin/stock`.
2. Selecciona componentes de la tabla.
3. Completa tipo, descripcion tecnica, marca, modelo, capacidad, remito, orden de compra,
   proveedor u observaciones.
4. Usa "Aplicar a seleccionados".
5. `StockPageController` llama a `StockService.actualizarDatosAdministrativosEnLote`.
6. Solo se actualizan los campos informados; los campos vacios no pisan datos existentes.
7. Si el lote deja de estar `PENDIENTE` y tiene descripcion real, `datos_completos` pasa a `true`.

### Uso de stock en tareas e instalacion en equipo

Flujo completo de una pieza de taller:

1. Una tarea tecnica puede registrar piezas usadas desde stock DISPONIBLE.
2. Al registrar el uso desde la tarea, el stock pasa de DISPONIBLE a RESERVADO (`reservar()`).
3. El tecnico puede luego ejecutar la accion "Instalar en equipo" sobre cada pieza RESERVADA.
4. Al instalar:
   - El `StockComponente` pasa de RESERVADO a ASIGNADO (`asignar()`).
   - Se crea un `Componente` en la tabla `componentes` con `origen = STOCK` y
     `estadoComparacion = ESPERADO`, vinculado al equipo de la tarea.
   - Se registra en auditoria de TAREAS y COMPONENTES.
5. La instalacion requiere que la tarea tenga un equipo real (no PC-GENERICA) asociado.
   Si la tarea esta en PC-GENERICA, reasignarla primero con la accion de reasignacion de equipo.
6. El sincronizador de stock huerfano respeta el estado ASIGNADO mientras el Componente este activo.

Estados de stock posibles:
- DISPONIBLE: pieza en el taller, lista para usar.
- RESERVADO: apartada por una tarea, no disponible para otras tareas.
- ASIGNADO: instalada fisicamente en un equipo real y registrada como Componente.

El stock disponible para tareas no muestra piezas RESERVADAS ni ASIGNADAS.

### APK LAN y actualizacion

1. La APK muestra pantallas del servidor LAN.
2. El servidor publica metadata y descarga de APK.
3. Si hay version nueva, la pantalla movil muestra "APK disponible".
4. La actualizacion puede disparar descarga/instalacion desde Android.
5. La APK actual no debe depender de Play Store.

### Tareas dictadas desde APK

1. El tecnico puede dictar el problema desde la APK.
2. Antes de guardar debe poder indicar manualmente o por busqueda predictiva quien solicito la
   ayuda.
3. La busqueda predictiva usa `/api/v1/movil/usuarios-dominio?q=...` y consulta AD cuando esta
   disponible.
4. Si AD no responde o no hay coincidencia, el campo acepta carga manual para no frenar el trabajo.
5. Los comentarios agregados desde la APK deben aparecer en el visor publico
   `/admin/tareas/visor`.
6. El tecnico puede comentar tareas donde es responsable o que fueron creadas por el desde la APK,
   cubriendo tareas historicas que pudieron quedar sin responsable.

## 7. Base de datos

Motor principal: MySQL.

Tablas clave:

- `usuarios`
- `roles`
- `usuario_roles`
- `modulos`
- `permisos`
- `equipos`
- `componentes`
- `stock_componentes`
- `tareas_tecnicas`
- `tareas_stock_uso`
- `tareas_avisos`
- `auditoria_eventos`
- `ubicaciones`
- `muebles`
- `bienes_patrimoniales`
- `actas`

Migraciones recientes relevantes:

| Migracion | Proposito |
|---|---|
| `V15__avisos_tareas_lan.sql` | Avisos para tareas LAN |
| `V16__permisos_tecnico_tareas_movil.sql` | Permisos moviles para tecnicos |
| `V18__stock_usado_en_tareas_y_avisos_dirigidos.sql` | Uso de stock en tareas y avisos dirigidos |
| `V19__permisos_tecnico_stock_movil.sql` | Permisos de Stock para tecnico movil |
| `V20__stock_ingresado_por.sql` | Columna `ingresado_por` en `stock_componentes` |
| `V21__stock_datos_completos.sql` | Columna `datos_completos` para separar pendientes |
| `V22__comentarios_creado_en_default.sql` | Default de `creado_en` en comentarios |
| `V23__instalar_stock_en_equipo.sql` | Indice de apoyo para instalacion de stock en equipo (flujo RESERVADO → ASIGNADO + Componente) |

En `local`, Hibernate usa `spring.jpa.hibernate.ddl-auto=update`. Eso puede agregar columnas
durante desarrollo, pero las migraciones SQL siguen siendo la referencia para produccion.

## 8. Desarrollo local

Comandos frecuentes desde la raiz del proyecto:

```powershell
.\mvnw.cmd --batch-mode test
.\mvnw.cmd --batch-mode "-Dtest=StockPageControllerTests,TareaMovilControllerTests" test
```

Servidor local con MySQL:

```powershell
$env:SPRING_PROFILES_ACTIVE='local'
$env:INVENTARIO_DB_PRIMARY_URL='jdbc:mysql://127.0.0.1:3306/inventario_modular'
$env:INVENTARIO_DB_PRIMARY_USER='inventario_local'
$env:INVENTARIO_DB_PRIMARY_PASSWORD='<clave-mysql-local>'
$env:INVENTARIO_DB_FALLBACK_URL='jdbc:mysql://127.0.0.1:3306/inventario_modular'
$env:INVENTARIO_DB_FALLBACK_USER='inventario_local'
$env:INVENTARIO_DB_FALLBACK_PASSWORD='<clave-mysql-local>'
$env:INVENTARIO_LOCAL_AUTH_ENABLED='true'
$env:INVENTARIO_LOCAL_AUTH_USERNAME='admin.local'
$env:INVENTARIO_LOCAL_AUTH_PASSWORD='<clave-admin-local>'
$env:INVENTARIO_LOCAL_DB_AUTH_ENABLED='true'
$env:INVENTARIO_LAN_ONLY='false'
.\mvnw.cmd spring-boot:run
```

URL local habitual:

```text
http://localhost:8081
```

Usuario local de laboratorio:

```text
admin.local / clave configurada en INVENTARIO_LOCAL_AUTH_PASSWORD
```

No documentar claves productivas reales en este archivo.

## 9. APK Android LAN

Proyecto:

```text
android/
```

Build:

```powershell
cd android
.\gradlew.bat assembleLanRelease
```

APK publicada/copias:

```text
output/android/tecnico-taller-san-pedro-lan-release.apk
```

Clases importantes:

- `MainActivity`: WebView, integracion con JS, descarga/actualizacion APK, escaneo.
- `PortraitCaptureActivity`: scanner en vertical.

La version LAN debe mantenerse coherente entre:

- `android/app/build.gradle`
- Metadata publicada por backend.
- Documentacion operativa.

## 10. Desarrollo, produccion y despliegue

Desarrollo Windows:

- Usar Java 21.
- Usar Maven Wrapper.
- Usar MySQL local cuando se prueben flujos reales.
- H2 sirve para tests y escenarios de casa, no para validar todo lo productivo.

Produccion Ubuntu:

- Revisar `docs/inventario-modular/despliegue-ubuntu-y-apk.md`.
- Revisar `docs/inventario-modular/actualizacion-produccion-inventario-modular.md`.
- Revisar `scripts/nginx/inventario-modular.conf`.
- El servicio productivo debe tratarse con cuidado: no tocar systemd/nginx/certificados del
  inventario viejo salvo que el pedido sea explicito.

Comandos y rutas productivas pueden cambiar; antes de ejecutar en produccion leer la guia vigente
y verificar fecha, host, rama y backup.

## 11. Decisiones tomadas

| Fecha aproximada | Decision | Motivo |
|---|---|---|
| 2026-08 | Construir Java/Spring desde cero | Evitar arrastrar deuda tecnica del inventario viejo |
| 2026-08 | Arquitectura API-first | Preparar app movil y automatizaciones |
| 2026-08 | Separar autenticacion de autorizacion | AD valida identidad; MySQL decide permisos |
| 2026-09 | Agregar APK LAN | Tecnicos necesitan operar desde celular en red local |
| 2026-09 | Scanner movil en vertical | Mejor uso real con celular |
| 2026-09 | Permitir Stock movil a tecnicos | Sumar piezas reales desde el taller |
| 2026-09 | Stock disponible implica taller | Evitar pedir ubicacion redundante o confusa |
| 2026-09 | Guardar `ingresado_por` | Saber que tecnico sumo cada componente |
| 2026-09 | Actualizacion administrativa en lote | Mismo remito/OC/proveedor para muchos componentes |
| 2026-09 | Escaneo rapido con pendientes | Cargar 20 monitores o 60 cartuchos sin tipear datos repetidos |
| 2026-09 | Autenticacion local de base activa en perfil local | La APK debe aceptar tecnicos locales como `usuario3` durante pruebas LAN |
| 2026-09 | Institucional Justicia Jujuy como UI vigente | Mockup aprobado como referencia literal: cabecera judicial azul, sidebar operativo sin marca duplicada, recuadros cuadrados |
| 2026-09 | Metodologia ITAM/CMDB de taller | Ordenar activos, configuracion, gemelos, stock, tareas y evidencia |

## 12. Que no tocar sin cuidado

- No revertir migraciones ya aplicadas.
- No borrar columnas existentes sin plan de migracion y backup.
- No cambiar permisos/roles sin revisar `AuthorizationService`, usuarios reales y tests.
- No agregar modulos o pantallas sueltas sin ubicarlas en el mapa ITAM/CMDB: activo,
  configuracion esperada, configuracion detectada, gemelo, diferencia, stock, trabajo tecnico,
  evidencia o reporte de decision.
- No convertir `usuario3` en lector si el pedido vigente es que sea tecnico.
- No desactivar `inventario.local-db-auth.enabled` en el perfil local sin probar login movil de
  un tecnico local; si se desactiva, usuarios como `usuario3` dejan de entrar por la APK.
- No mezclar identidad AD con autorizacion local: son responsabilidades distintas.
- No guardar claves de Active Directory en Inventario Modular.
- No publicar la APK anonimamente; debe respetar login/permisos.
- No romper `/movil/tareas` ni `/movil/stock`, porque la APK depende de esas rutas.
- No cambiar el contrato de `/api/v1/stock/componentes` sin revisar la APK/pantalla movil.
- No cerrar `/admin/tareas/visor`: es un visor publico de solo lectura. Crear tareas,
  comentar, tomar, cambiar estado, usar stock o eliminar requiere sesion y rol administrador.
- No volver a mostrar "deposito" o "ubicacion fisica" como dato principal de stock disponible.
- No eliminar `ingresado_por`; es clave para trazabilidad de altas desde celular.
- No eliminar `datos_completos` ni `TipoComponente.PENDIENTE`; sostienen el flujo de escaneo rapido.
- No tocar despliegue productivo, nginx, certificados o systemd sin confirmar entorno y backup.
- No asumir que H2 representa todos los casos de MySQL.
- No hacer refactors grandes mientras se esta corrigiendo una pantalla o flujo puntual.
- No dejar pantallas con solapas redundantes: en Stock, la edicion debe estar por registro.
- No cerrar cambios de UI sin revisar visualmente tabla, columna Tipo, mobile y desktop.
- No volver a una UI generica o ajena al mockup `justicia-jujuy-inventario.html` sin pedido
  explicito; el tema vigente es Institucional Justicia Jujuy.
- No usar emoji como lenguaje principal de navegacion del admin; preferir codigos cortos o iconos
  SVG consistentes.
- El panel general `/admin` ya no usa la "Brujula Operativa"; es un panel de decisiones con
  cola tecnica, stock disponible, stock incompleto, diferencias del gemelo, equipos, modo activo,
  APK y atajos criticos.
- La navegacion principal ya no muestra el bloque "Bienes y Sedes"; no reintroducirlo sin pedido
  explicito del usuario.
- No documentar IPs internas, usuarios reales ni claves literales. Usar placeholders como
  `MYSQL_INTERNO_IP`, `AD_INTERNO_IP`, `APP_INTERNA_IP`, `usuario.tecnico` y
  `CAMBIAR_CLAVE_LOCAL_SEGURA`.
- El login web y movil tiene bloqueo temporal por intentos fallidos mediante
  `LoginAttemptService`; ajustar `INVENTARIO_LOGIN_MAX_FAILURES` e
  `INVENTARIO_LOGIN_BLOCK_SECONDS` si cambia la politica.
- En configuracion base/productiva, Hibernate debe validar esquema (`ddl-auto=validate`) y Flyway
  debe estar habilitado. El perfil `local` puede conservar `ddl-auto=update` para desarrollo con
  MySQL local.

## 13. Tests recomendados por area

Stock y movil:

```powershell
.\mvnw.cmd --batch-mode "-Dtest=StockPageControllerTests,TareaMovilControllerTests" test
```

Tareas:

```powershell
.\mvnw.cmd --batch-mode "-Dtest=TareaTecnicaControllerTests,TareaMovilControllerTests" test
```

Suite completa:

```powershell
.\mvnw.cmd --batch-mode test
```

APK:

```powershell
cd android
.\gradlew.bat assembleLanRelease
```

## 14. Documentos auxiliares

Leer estos documentos segun la tarea:

- `README.md`: vision general y uso del proyecto.
- `CONTEXT.md`: contexto historico corto.
- `ARQUITECTURA_VISUAL_Y_FUNCIONAL.md`: arquitectura visual/funcional.
- `docs/inventario-modular/bitacora-del-proyecto.md`: historia larga del proyecto.
- `docs/decisions/`: decisiones arquitectonicas.
- `docs/inventario-modular/despliegue-ubuntu-y-apk.md`: despliegue Ubuntu y APK.
- `docs/inventario-modular/actualizacion-produccion-inventario-modular.md`: actualizacion productiva.
- `docs/inventario-modular/usuarios-locales-y-active-directory.md`: usuarios locales y AD.
- `docs/inventario-modular/seguridad-modular-inicial.md`: permisos y seguridad.
- `docs/inventario-modular/metodologia-itam-cmdb-taller.md`: mapa ITAM/CMDB de taller.
- `docs/decisions/ADR-009-metodologia-itam-cmdb-de-taller.md`: decision arquitectonica.

## 15. Como debe trabajar una IA en este repo

1. Leer `AI_CONTEXT.md`.
2. Revisar `git status`.
3. No revertir cambios ajenos.
4. Buscar con `rg`.
5. Leer clases y templates antes de editar.
6. Hacer cambios pequenos y coherentes con el patron existente.
7. Agregar o ajustar tests si el cambio toca reglas de negocio.
8. Ejecutar tests enfocados.
9. Si hay UI, revisar `docs/arquitectura_visual.md` y mantener Institucional Justicia Jujuy
   salvo pedido explicito de otra direccion.
10. Si hay UI, verificar por navegador o HTTP al menos la ruta principal.
11. Si se pide contexto de repo, usar Graphify primero cuando exista `graphify-out/graph.json`.
12. Al finalizar, decir que archivos se tocaron, que se probo y que queda pendiente.
