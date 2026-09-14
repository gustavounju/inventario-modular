# Arquitectura Visual: Institucional Justicia Jujuy

La arquitectura visual activa en `inventario-modular` se denomina **Institucional Justicia Jujuy**. El sistema debe reconocerse como una herramienta interna del **Centro Judicial San Pedro** para el **Taller Informatica**, con una estetica sobria, clara y administrativa inspirada en los sistemas judiciales provinciales.

## Principios de Diseno

La experiencia prioriza claridad diaria, lectura rapida, formularios previsibles y una identidad institucional visible. Debe sentirse como un sistema serio de trabajo, no como una landing page ni como una interfaz decorativa.

### 1. Colores y Esquema

- **Azul institucional**: `#006f99`, usado en cabeceras, sidebar, acciones principales y estado activo.
- **Azul profundo**: `#005777` y `#014760`, usados para contraste, hover y texto destacado.
- **Fondo general**: `#dceef7`, celeste claro de sistema administrativo.
- **Superficies**: `#f8fcfe`, `#eaf5fa` y `#edf7fb`, para paneles, filtros y cabeceras de seccion.
- **Bordes**: `#b8cdd7`, con inputs en `#7f969f` para conservar forma de formulario institucional.
- **Alertas**: rojo `#c5142d` solo para errores, campos criticos o acciones destructivas.

### 2. Disposicion Visual

- **Shell Administrativo**: `.shell-wide` mantiene sidebar fijo, topbar compacta y area de contenido amplia.
- **Sidebar Institucional**: `.app-sidebar` identifica el sistema como `Centro Judicial San Pedro` y `Taller Informatica`.
- **Topbar de Trabajo**: `admin/nav` concentra titulo de modulo, sesion activa, APK y selector de modo.
- **Paneles de Trabajo**: `.status-panel`, `.modules-section`, `.task-board` y `.task-create-panel` usan fondos claros, bordes finos y sombras contenidas.
- **Tablas y Filtros**: deben ser densos pero legibles, con encabezados celestes y foco visible.
- **APK/WebView**: `/movil/tareas`, `/movil/stock` y `/movil/login` comparten azul institucional, marca `Centro Judicial San Pedro` y subtitulo `Taller Informatica`.

### 3. Reusabilidad y Estandarizacion

Para nuevos modulos, reutilizar estas piezas:

- `.shell` y `.shell-wide`: contenedores base.
- `.panel-topbar` y fragmento `admin/nav`: navegacion, identidad y sesion comun.
- `.status-panel`, `.modules-section`, `.inline-edit-card`: areas principales de trabajo.
- `.primary-action`, `.secondary-action`, `.button-danger-subtle`: acciones del sistema.
- `.responsive-table`, `.summary-grid`, `.workflow-banner`: listados, metricas y guias de flujo.

## Reglas de Evolucion

1. Mantener visible el nombre **Centro Judicial San Pedro**.
2. Usar **Taller Informatica** para identificar el area tecnica.
3. No volver a "Stock de Deposito" para piezas disponibles: usar Stock del Taller.
4. No introducir paletas dominantes ajenas al azul institucional sin pedido explicito.
5. Antes de aplicar cambios grandes, generar mockup en `output/mockups-diseno/`.
6. Verificar visualmente web y APK/WebView despues de cambios de tema.
