-- V23: Instalar stock desde tarea en equipo real
--
-- Este flujo no requiere DDL nuevo: utiliza las tablas existentes:
--   - tareas_stock_usos: registro del uso de stock en la tarea
--   - stock_componentes: pieza fisica (estado pasa de RESERVADO a ASIGNADO)
--   - componentes: componente oficial del equipo (se crea al instalar)
--
-- Regla de negocio documentada:
--   DISPONIBLE -> (tomar en tarea) -> RESERVADO -> (instalar en equipo) -> ASIGNADO
--   Al instalar, se crea un Componente con origen=STOCK y estado_comparacion=ESPERADO.
--   El sincronizador de stock huerfano respeta el estado ASIGNADO mientras el Componente este activo.

-- Indice de apoyo para busquedas de uso por id de tarea (optimiza la consulta de instalacion)
CREATE INDEX IF NOT EXISTS idx_tareas_stock_usos_tarea_id ON tareas_stock_usos (tarea_id);
