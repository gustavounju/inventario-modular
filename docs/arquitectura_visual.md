# Arquitectura Visual: Institucional Justicia Jujuy

La arquitectura visual activa en `inventario-modular` se denomina **Institucional Justicia Jujuy**. El sistema debe reconocerse como una herramienta interna del **Centro Judicial San Pedro** para el **Taller Informatica**, con una estetica sobria, clara y administrativa tomada del mockup aprobado `output/mockups-diseno/justicia-jujuy-inventario.html`.

## Principios de Diseno

La experiencia prioriza claridad diaria, lectura rapida, formularios previsibles y una identidad institucional visible. Debe sentirse como un sistema serio de trabajo, no como una landing page ni como una interfaz decorativa.

### 1. Colores y Esquema

- **Azul institucional**: `#006f99`, usado en la cabecera superior, acciones principales y estado activo.
- **Azul profundo**: `#005777` y `#014760`, usados para contraste, hover y texto destacado.
- **Fondo general**: `#dceef7`, celeste claro de sistema administrativo.
- **Superficies**: `#f8fcfe`, `#eaf5fa` y `#edf7fb`, para paneles, filtros y cabeceras de seccion.
- **Bordes**: `#b8cdd7`, con inputs en `#7f969f` para conservar forma de formulario institucional.
- **Alertas**: rojo `#c5142d` solo para errores, campos criticos o acciones destructivas.
- **Modo visual**: el mockup aprobado es claro; si el navegador conserva `data-theme="dark"`,
  las variables deben seguir resolviendo a la misma paleta institucional clara.

### 2. Disposicion Visual

- **Cabecera Judicial**: `admin/nav` muestra arriba `Poder Judicial de la Provincia de Jujuy` con fondo azul `#006f99`, tipografia serif italica y navegacion horizontal.
- **Shell Administrativo**: `.shell-wide` queda debajo de la cabecera judicial, con sidebar fijo y area de contenido amplia.
- **Sidebar Operativo**: `.app-sidebar` no repite `Centro Judicial San Pedro` ni `Taller Informatica`; empieza con `Sistema de inventario` y solo lista modulos/acciones.
- **Topbar de Trabajo**: `.panel-topbar` concentra titulo de modulo, sesion activa, APK y selector de modo sin fondos pesados ni sombras decorativas.
- **Paneles de Trabajo**: `.status-panel`, `.modules-section`, `.task-board` y `.task-create-panel` usan fondos claros, bordes finos, esquinas cuadradas y sombras contenidas solo en contenedores grandes.
- **Modulos heredados**: tarjetas, badges, tablas, formularios y banners de pantallas anteriores
  deben normalizarse a fondo claro, borde fino y esquina cuadrada para no mezclar estilos.
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

1. Mantener visible el nombre **Centro Judicial San Pedro** donde corresponda al contexto institucional, pero no repetirlo en el sidebar.
2. Usar **Taller Informatica** para identificar el area tecnica, sin duplicarlo dentro del panel lateral.
3. No volver a "Stock de Deposito" para piezas disponibles: usar Stock del Taller.
4. No introducir paletas dominantes ajenas al azul institucional sin pedido explicito.
5. Antes de aplicar cambios grandes, generar mockup en `output/mockups-diseno/` y aplicar el resultado de forma literal cuando el usuario lo apruebe.
6. Verificar visualmente web y APK/WebView despues de cambios de tema.
