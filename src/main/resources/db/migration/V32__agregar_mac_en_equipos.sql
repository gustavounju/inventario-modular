-- =========================================================================
-- V32: Agregar columna MAC en tabla equipos para telemetría de red
-- =========================================================================

ALTER TABLE equipos ADD COLUMN mac VARCHAR(45) NULL AFTER ip;
