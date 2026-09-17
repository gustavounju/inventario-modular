CREATE TABLE IF NOT EXISTS sistema_configuracion (
  clave VARCHAR(120) NOT NULL,
  valor TEXT NULL,
  actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (clave)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO modulos (codigo, nombre, descripcion, orden)
SELECT 'SISTEMA', 'Sistema', 'Configuracion tecnica del sistema, Active Directory y modo de arranque.', 100
WHERE NOT EXISTS (SELECT 1 FROM modulos WHERE codigo = 'SISTEMA');

INSERT INTO rol_modulo_permisos (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.codigo = 'ADMINISTRADOR'
  AND m.codigo = 'SISTEMA'
  AND p.codigo IN ('VER', 'ADMINISTRAR')
  AND NOT EXISTS (
    SELECT 1
    FROM rol_modulo_permisos rmp
    WHERE rmp.rol_id = r.id
      AND rmp.modulo_id = m.id
      AND rmp.permiso_id = p.id
  );
