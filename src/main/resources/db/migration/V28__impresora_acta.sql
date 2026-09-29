-- V28: Gestión de impresoras para el acta
-- Marca cuál es la impresora activa del puesto (solo una por equipo)
ALTER TABLE componentes ADD COLUMN es_impresora_activa BOOLEAN NOT NULL DEFAULT FALSE;

-- Indica que el equipo imprime en red (no tiene impresora local asignada)
ALTER TABLE equipos ADD COLUMN impresora_en_red BOOLEAN NOT NULL DEFAULT FALSE;
