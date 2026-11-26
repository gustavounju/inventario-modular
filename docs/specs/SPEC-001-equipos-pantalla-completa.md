# SPEC-001: Modo Modal a Pantalla Completa para Módulo de Equipos (`/admin/equipos`)

- **Estado:** IMPLEMENTADO (Paso 4 SDD - Validado)
- **Fecha:** 2026-11-26
- **Módulo afectado:** `equipos` (`/admin/equipos`)
- **Requiere migración Flyway:** No (cambio 100 % de capa de presentación UI/UX en Thymeleaf, CSS y JS)

---

## 1. Problema y Objetivo Funcional

Actualmente, al ingresar a `http://10.15.1.80:8081/admin/equipos`:
- La barra lateral izquierda (`260px`), los márgenes de `.shell-wide` (`72px`), el límite `max-width: 1600px` y los encabezados altos consumen gran parte de la pantalla.
- Como el módulo de Equipos ahora muestra el **Plano Visual por Fueros/Vocalías/Pisos**, conexiones de impresoras y tablas con múltiples características técnicas de cada equipo, el espacio horizontal y vertical resulta reducido.

**Objetivo:**
Cuando el usuario haga clic en la opción **Equipos** (`/admin/equipos`), el módulo debe abrirse automáticamente como un **Modal / Lienzo a Pantalla Completa (`100vw × 100vh`)**, dedicando todo el ancho y alto del monitor exclusivamente a visualizar los fueros, oficinas y características de los equipos.

---

## 2. Diseño de Experiencia de Usuario (UX/UI)

1. **Apertura automática a Pantalla Completa al entrar a `/admin/equipos`:**
   - Al cargar `/admin/equipos`, el contenedor principal activa por defecto la clase `.equipos-fullscreen-modal`.
   - Oculta la barra lateral (`#app-sidebar`) y la cabecera global institucional, ocupando el `100%` del viewport (`position: fixed; inset: 0; z-index: 1100; overflow-y: auto`).
   - Elimina el tope de `max-width: 1600px` para usar el `100%` del ancho real del monitor.

2. **Barra superior compacta del Modal (Todo en una sola línea):**
   - **Izquierda:** Botón `☰ Menú` (para abrir el cajón lateral de navegación sin perder el modo pantalla completa) + Título `Inventario de Equipos` + Píldoras compactas de métricas (`Total`, `Sincronizados`, `Pendientes`, `En Taller`).
   - **Derecha:** Botón **`🗕 Vista Normal / Minimizar`** (o tecla `Esc`) por si el operador desea contraer el modal al layout tradicional con barra lateral fija, y acceso directo **`⬅ Panel General`** (`/admin`).

3. **Mayor espacio para Fueros y Características de los Equipos:**
   - El lienzo del **Plano Físico (`#plano-stage`)** y la **Tabla Clásica (`#vista-tabla-container`)** ganan `+300px` de ancho adicional y `+140px` de alto visible inmediato.
   - Las tarjetas de equipo en el plano y las filas de la tabla aprovechan el ancho completo para mostrar sin recortes: **Fuero, Vocalía/Oficina, Nombre de Host, IP, MAC, Usuario AD, Sistema Operativo, Impresora vinculada y Estado del Gemelo Digital**.

4. **Consola de Taller Combinada (Mosaico de Fueros + Explorador Maestro-Detalle + Matriz Técnica):**
   - **Paleta 100 % Institucional Sobria:** Se elimina la paleta multicolor (`FUERO_PALETTE`) de los textos de los fueros. Todos los títulos, encabezados y etiquetas usan **Azul Marino / Pizarra Institucional (`#0f172a` / `#0284c7`)** en modo claro y **Blanco / Celeste Institucional (`#f8fafc` / `#38bdf8`)** en modo nocturno. Los colores vivos se reservan exclusivamente para los semáforos técnicos (`🟢 Sincronizado`, `🟡 Pendiente`, `🔵 Impresora`, `🟠 Taller`).
   - **Nivel 1 — Mosaico General de Fueros:** Al entrar a `/admin/equipos`, se presenta una grilla de tarjetas institucionales (una por Fuero) que permite visualizar todos los fueros en una sola pantalla sin scroll largo, con contadores de dependencias, PCs, impresoras, barra de sincronización de gemelo digital y chips de sus vocalías/oficinas.
   - **Nivel 2 — Explorador Maestro-Detalle + Matriz de Taller / Croquis:** Al hacer clic en un Fuero (o buscar texto arriba), se abre una vista de 2 columnas con el **Directorio Lateral de Fueros (`260px`)** a la izquierda (para cambiar de fuero en 1 clic) y el detalle a la derecha, alternable entre **📊 Matriz de Taller** (tabla técnica alineada por oficina con Host, Usuario AD, IP/MAC, SO, Impresora, Estado y botones directos a Ficha/Gemelo/Acta) y **🔌 Croquis y Conexiones** (tarjetas de puesto con líneas SVG a impresoras de red).

5. **Compatibilidad con Sub-modales (`<dialog>`):**
   - Los diálogos nativos existentes (`#modal-impresora`, `#modal-taller`, `#modal-editar-impresora`, `#modal-vinculados`) y el tooltip flotante (`#plano-floating-tooltip`) mantienen un `z-index` superior para abrirse limpiamente por encima del modal de pantalla completa.
   - Al redimensionar o alternar el modo pantalla completa, `plano-equipos.js` dispara `requestAnimationFrame(drawAmbientConnections)` para reajustar las líneas SVG entre PCs e impresoras.

---

## 3. Archivos a Modificar

1. `src/main/resources/templates/admin/equipos.html`:
   - Incorporar la cabecera/controles del modo modal a pantalla completa (activado por defecto al entrar a `/admin/equipos`).
2. `src/main/resources/static/css/plano-equipos.css`:
   - Agregar las reglas de `.equipos-fullscreen-modal` (modo claro y modo oscuro `data-theme="dark"`), compactación de tarjetas KPI en pantalla completa y expansión al `100%` del ancho para fueros y características.
3. `src/main/resources/static/js/plano-equipos.js`:
   - Manejar la apertura por defecto en pantalla completa, el botón de alternar (`Pantalla Completa` / `Vista con Menú`), soporte de tecla `Esc` (cuando no haya un `<dialog>` abierto) y recálculo automático del SVG de conexiones.

---

## 4. Criterios de Aceptación y Verificación (Tests)

- [ ] Al hacer clic en **Equipos** (`/admin/equipos`), el módulo se despliega a pantalla completa (`100%` de ancho y alto) sin la barra lateral fija ocupando espacio.
- [ ] El usuario puede abrir el menú con un botón o volver al Panel General con 1 clic.
- [ ] Los sub-modales (`Registrar Impresora`, `Armar PC Taller`, `Editar Impresora`, `Ver vinculados`) y el tooltip de características siguen funcionando encima de la pantalla completa.
- [ ] Las líneas SVG de conexión entre equipos e impresoras se dibujan en las coordenadas correctas a pantalla completa.
- [ ] `sh ./mvnw --batch-mode test` pasa con `BUILD SUCCESS`.
