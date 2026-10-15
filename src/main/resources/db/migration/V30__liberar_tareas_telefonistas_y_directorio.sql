-- =========================================================================
-- V26: Directorio telefónico de internos y liberación de tareas de telefonistas
-- =========================================================================

-- 1. Tabla de directorio de internos telefónicos para resolver números (ej. 2005 -> MesaDeEntradasInformatica)
CREATE TABLE IF NOT EXISTS directorio_telefonico (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    departamento VARCHAR(120) NULL,
    origen VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_dir_tel_numero (numero)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Carga inicial de internos conocidos del Centro Judicial San Pedro
INSERT INTO directorio_telefonico (numero, nombre, departamento, origen) VALUES
('2005', 'MesaDeEntradasInformatica', 'Mesa de Entradas Informática', 'SISTEMA'),
('2000', 'Mesa de Entradas General', 'Mesa de Entradas', 'SISTEMA'),
('1005', 'Taller Informatica', 'Taller de Informática', 'SISTEMA')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), departamento = VALUES(departamento);

-- 3. Liberar cualquier tarea técnica abierta que haya quedado asignada erróneamente a un usuario telefonista
-- Las telefonistas solo publican tareas para que los técnicos las tomen
UPDATE tareas_tecnicas tt
JOIN usuarios u ON u.username = tt.responsable
JOIN usuario_roles ur ON ur.usuario_id = u.id
JOIN roles r ON r.id = ur.rol_id
SET tt.responsable = NULL,
    tt.estado = 'PENDIENTE'
WHERE r.codigo = 'TELEFONISTA'
  AND tt.estado IN ('PENDIENTE', 'EN_PROCESO');
