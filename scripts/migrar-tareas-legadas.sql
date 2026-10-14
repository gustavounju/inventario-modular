-- =====================================================================
-- MIGRACIÓN DE TAREAS Y COMENTARIOS: inventario_dev -> inventario_modular
-- Servidor: 10.15.0.62 (MySQL 8.0)
-- Ejecutado con éxito el 2026-10-14
-- Migradas: 652 tareas y 109 comentarios/acciones
-- =====================================================================

-- 1. Asegurar que exista el equipo auxiliar PC-GENERICA en inventario_modular
INSERT INTO inventario_modular.equipos (nombre, ultimo_usuario, fuero, ubicacion, sistema_operativo, monitoreo, activo)
SELECT 'PC-GENERICA', 'Sin asignar', 'Sin fuero informado', 'Mesa de ayuda', 'No aplica', 'AUXILIAR_TAREAS', TRUE
WHERE NOT EXISTS (
  SELECT 1
  FROM inventario_modular.equipos
  WHERE UPPER(nombre) = 'PC-GENERICA'
);

-- 2. Migrar las tareas desde inventario_dev.tasks hacia inventario_modular.tareas_tecnicas
INSERT INTO inventario_modular.tareas_tecnicas (
    id,
    equipo_id,
    titulo,
    descripcion,
    estado,
    prioridad,
    responsable,
    creado_por,
    solicitante_username,
    solicitante_nombre,
    solicitante_fuero,
    observaciones_cierre,
    cerrado_en,
    creado_en,
    actualizado_en
)
SELECT 
    t.id,
    -- Asociar al ID del equipo por nombre (pc_name); si no existe o es nulo, enlaza a PC-GENERICA
    COALESCE(
        (SELECT eq.id FROM inventario_modular.equipos eq WHERE UPPER(TRIM(eq.nombre)) = UPPER(TRIM(t.pc_name)) LIMIT 1),
        (SELECT eq.id FROM inventario_modular.equipos eq WHERE UPPER(eq.nombre) = 'PC-GENERICA' LIMIT 1)
    ) AS equipo_id,
    -- Extrae la primera línea de la descripción para el título (máx 180 chars)
    LEFT(
        COALESCE(
            NULLIF(TRIM(SUBSTRING_INDEX(REPLACE(REPLACE(t.descripcion, '\r\n', '\n'), '\r', '\n'), '\n', 1)), ''),
            NULLIF(TRIM(t.categoria), ''),
            'Tarea migrada'
        ),
        180
    ) AS titulo,
    -- Descripción completa (máx 1000 chars)
    LEFT(COALESCE(t.descripcion, ''), 1000) AS descripcion,
    -- Mapeo de estados del sistema viejo al enum de Java
    CASE 
        WHEN UPPER(TRIM(t.estado)) IN ('HECHA', 'CERRADA', 'COMPLETADA', 'RESUELTA') THEN 'CERRADA'
        WHEN UPPER(TRIM(t.estado)) IN ('ASIGNADA', 'EN PROGRESO', 'EN_PROCESO', 'TOMADA') THEN 'EN_PROCESO'
        WHEN UPPER(TRIM(t.estado)) IN ('CANCELADA', 'ANULADA') THEN 'CANCELADA'
        ELSE 'PENDIENTE'
    END AS estado,
    -- Mapeo de prioridades
    CASE 
        WHEN t.prioridad IN (4, '4') OR UPPER(TRIM(t.prioridad)) IN ('URGENTE', 'CRITICA', 'CRÍTICA') THEN 'URGENTE'
        WHEN t.prioridad IN (3, '3') OR UPPER(TRIM(t.prioridad)) = 'ALTA' THEN 'ALTA'
        WHEN t.prioridad IN (1, '1') OR UPPER(TRIM(t.prioridad)) = 'BAJA' THEN 'BAJA'
        ELSE 'MEDIA'
    END AS prioridad,
    -- Responsable técnico
    LEFT(
        COALESCE(
            NULLIF(TRIM(t.assigned_to), ''),
            NULLIF(TRIM(t.completed_by), '')
        ),
        120
    ) AS responsable,
    -- Creado por
    LEFT(COALESCE(NULLIF(TRIM(t.solicitante), ''), 'sistema_legado'), 120) AS creado_por,
    -- Solicitante
    LEFT(COALESCE(NULLIF(TRIM(t.solicitante), ''), 'sin-solicitante'), 120) AS solicitante_username,
    LEFT(COALESCE(NULLIF(TRIM(t.solicitante), ''), 'Sin solicitante informado'), 180) AS solicitante_nombre,
    LEFT(COALESCE(NULLIF(TRIM(t.fuero), ''), 'Sin fuero informado'), 120) AS solicitante_fuero,
    -- Resolución técnica
    LEFT(COALESCE(t.solucion, ''), 1000) AS observaciones_cierre,
    -- Fechas de cierre y creación originales
    t.completed_at AS cerrado_en,
    COALESCE(t.created_at, CURRENT_TIMESTAMP) AS creado_en,
    COALESCE(t.completed_at, t.created_at, CURRENT_TIMESTAMP) AS actualizado_en
FROM inventario_dev.tasks t
WHERE NOT EXISTS (
    SELECT 1 FROM inventario_modular.tareas_tecnicas dest WHERE dest.id = t.id
);

-- 3. Migrar las notas históricas (task_actions) como comentarios de tareas
INSERT INTO inventario_modular.tareas_tecnicas_comentarios (
    tarea_id,
    autor,
    comentario,
    creado_en
)
SELECT 
    a.task_id,
    LEFT(COALESCE(NULLIF(TRIM(a.user_name), ''), 'Desconocido'), 120) AS autor,
    LEFT(COALESCE(a.action_text, ''), 1000) AS comentario,
    COALESCE(a.created_at, CURRENT_TIMESTAMP) AS creado_en
FROM inventario_dev.task_actions a
INNER JOIN inventario_modular.tareas_tecnicas tt ON tt.id = a.task_id
WHERE NOT EXISTS (
    SELECT 1 
    FROM inventario_modular.tareas_tecnicas_comentarios tc 
    WHERE tc.tarea_id = a.task_id 
      AND tc.autor = LEFT(COALESCE(NULLIF(TRIM(a.user_name), ''), 'Desconocido'), 120)
      AND tc.comentario = LEFT(COALESCE(a.action_text, ''), 1000)
);

-- 4. Ajustar el AUTO_INCREMENT para futuras tareas creadas en la app
SET @max_id = (SELECT IFNULL(MAX(id), 0) + 1 FROM inventario_modular.tareas_tecnicas);
SET @sql_ai = CONCAT('ALTER TABLE inventario_modular.tareas_tecnicas AUTO_INCREMENT = ', @max_id);
PREPARE stmt FROM @sql_ai;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. Ajustar el AUTO_INCREMENT de comentarios
SET @max_cid = (SELECT IFNULL(MAX(id), 0) + 1 FROM inventario_modular.tareas_tecnicas_comentarios);
SET @sql_cai = CONCAT('ALTER TABLE inventario_modular.tareas_tecnicas_comentarios AUTO_INCREMENT = ', @max_cid);
PREPARE stmt_c FROM @sql_cai;
EXECUTE stmt_c;
DEALLOCATE PREPARE stmt_c;
