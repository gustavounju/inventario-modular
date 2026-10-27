-- =========================================================================
-- V33: Tipo de tarea tecnica (incidente vs trabajo interno del taller) y
--      ciclo de vida ITAM del equipo (EN_ARMADO -> DISPONIBLE -> EN_SERVICIO).
--
-- Idempotente: en prod/trabajo este script corre en CADA arranque via
-- spring.sql.init, por eso cada ALTER se protege consultando information_schema.
-- =========================================================================

SET @existe_tipo := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tareas_tecnicas' AND COLUMN_NAME = 'tipo');
SET @sql_tipo := IF(@existe_tipo = 0,
    'ALTER TABLE tareas_tecnicas ADD COLUMN tipo VARCHAR(40) NOT NULL DEFAULT ''INCIDENTE'' AFTER equipo_id',
    'SELECT 1');
PREPARE stmt_tipo FROM @sql_tipo;
EXECUTE stmt_tipo;
DEALLOCATE PREPARE stmt_tipo;

SET @existe_ciclo := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'equipos' AND COLUMN_NAME = 'estado_ciclo');
SET @sql_ciclo := IF(@existe_ciclo = 0,
    'ALTER TABLE equipos ADD COLUMN estado_ciclo VARCHAR(40) NOT NULL DEFAULT ''EN_SERVICIO'' AFTER activo',
    'SELECT 1');
PREPARE stmt_ciclo FROM @sql_ciclo;
EXECUTE stmt_ciclo;
DEALLOCATE PREPARE stmt_ciclo;

-- Equipos ya creados en taller (ARMADO-xxx o TALLER-xxx) que nunca reportaron por script siguen en armado.
UPDATE equipos
SET estado_ciclo = 'EN_ARMADO'
WHERE estado_ciclo = 'EN_SERVICIO'
  AND (nombre LIKE 'ARMADO-%' OR nombre LIKE 'TALLER-%')
  AND ultimo_reporte_en IS NULL;
