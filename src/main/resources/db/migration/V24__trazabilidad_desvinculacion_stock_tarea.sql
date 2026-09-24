-- V24: Trazabilidad de desvinculacion de stock usado por tareas
--
-- Permite mostrar en la tarea que una pieza fue instalada/asignada a un equipo
-- y luego retirada o desvinculada, registrando usuario, motivo y equipo origen.

ALTER TABLE tareas_stock_usos ADD COLUMN desvinculado_en DATETIME NULL;
ALTER TABLE tareas_stock_usos ADD COLUMN desvinculado_por VARCHAR(120) NULL;
ALTER TABLE tareas_stock_usos ADD COLUMN desvinculacion_motivo VARCHAR(500) NULL;
ALTER TABLE tareas_stock_usos ADD COLUMN desvinculado_equipo_nombre VARCHAR(180) NULL;
