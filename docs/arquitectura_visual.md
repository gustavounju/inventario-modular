# Arquitectura Visual: Mesa Tecnica Oscura

La arquitectura visual activa en `inventario-modular` se denomina **Mesa Tecnica Oscura**: una interfaz de administracion tipo centro de control para taller e informatica, pensada para uso repetido, lectura rapida y trabajo diario con muchos datos.

## Principios de Diseno

La experiencia prioriza densidad operativa, contraste controlado y jerarquia clara. El sistema debe sentirse como una mesa de diagnostico: oscuro, preciso, sobrio y util.

### 1. Colores y Esquema

- **Fondo General**: `#050708` con grilla tecnica sutil para reforzar el contexto de control operativo.
- **Paneles y Superficies**: `#0c1417`, `#081113` y `#122225` para separar modulos sin llenar la pantalla de tarjetas pesadas.
- **Texto Principal**: `#eef8f5`; secundarios en `#9ab2b0` y metadatos en `#607977`.
- **Acento Primario**: `#26d3c5`, usado para acciones, enlaces, foco visible y estados activos.
- **Estados**: verde para disponibilidad, amarillo para pendientes o faltantes, rojo para errores y acciones destructivas.

### 2. Disposicion Visual

- **Shell Administrativo**: `.shell-wide` organiza la app como panel de trabajo con sidebar fijo, topbar compacta y area de contenido amplia.
- **Sidebar Operativo**: `.app-sidebar` agrupa los modulos por areas funcionales con codigos cortos (`EQ`, `ST`, `TT`, etc.) para lectura rapida.
- **Paneles de Trabajo**: `.status-panel`, `.modules-section`, `.task-board` y `.task-create-panel` usan fondos oscuros, bordes finos y sombras contenidas.
- **Datos Densos**: `.summary-grid`, `.runtime-grid`, `.identity-grid` y tablas responsivas favorecen comparacion rapida sin perder legibilidad.
- **Controles Repetibles**: formularios, filtros y acciones mantienen alturas compactas, foco accesible y contraste estable.

### 3. Reusabilidad y Estandarizacion

Para nuevos modulos, reutilizar estas piezas:

- `.shell` y `.shell-wide`: contenedores base.
- `.panel-topbar` y fragmento `admin/nav`: navegacion y sesion comun.
- `.status-panel`, `.modules-section`, `.inline-edit-card`: areas principales de trabajo.
- `.primary-action`, `.secondary-action`, `.button-danger-subtle`: acciones del sistema.
- `.responsive-table`, `.summary-grid`, `.workflow-banner`: listados, metricas y guias de flujo.

## Recomendacion de Nuevos Modulos

1. **Modulo de Redes**: switches, routers, IPs, VLANs y puertos.
2. **Modulo de Software y Licencias**: sistemas operativos, software instalado y vencimientos.
3. **Modulo de Mantenimiento y Tickets**: incidentes, mantenimientos preventivos y correctivos.
4. **Modulo de Reportes y Auditoria**: trazabilidad de cambios y exportacion PDF/Excel.
