-- =========================================================================
-- V33: Tipo de tarea tecnica (incidente vs trabajo interno del taller) y
--      ciclo de vida ITAM del equipo (EN_ARMADO -> DISPONIBLE -> EN_SERVICIO).
-- =========================================================================

ALTER TABLE tareas_tecnicas ADD COLUMN tipo VARCHAR(40) NOT NULL DEFAULT 'INCIDENTE' AFTER equipo_id;

ALTER TABLE equipos ADD COLUMN estado_ciclo VARCHAR(40) NOT NULL DEFAULT 'EN_SERVICIO' AFTER activo;

-- Equipos ya creados en taller (ARMADO-xxx o TALLER-xxx) que nunca reportaron por script siguen en armado.
UPDATE equipos
SET estado_ciclo = 'EN_ARMADO'
WHERE estado_ciclo = 'EN_SERVICIO'
  AND (nombre LIKE 'ARMADO-%' OR nombre LIKE 'TALLER-%')
  AND ultimo_reporte_en IS NULL;

