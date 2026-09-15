INSERT INTO roles (codigo, nombre, descripcion)
SELECT 'TELEFONISTA', 'Telefonista', 'Operador de Mesa de Ayuda: recibe llamados y publica tareas para tecnicos.'
WHERE NOT EXISTS (
  SELECT 1
  FROM roles r
  WHERE r.codigo = 'TELEFONISTA'
);

INSERT INTO rol_modulo_permisos (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.codigo = 'TELEFONISTA'
  AND m.codigo = 'TAREAS'
  AND p.codigo IN ('VER', 'CREAR')
  AND NOT EXISTS (
    SELECT 1
    FROM rol_modulo_permisos rmp
    WHERE rmp.rol_id = r.id
      AND rmp.modulo_id = m.id
      AND rmp.permiso_id = p.id
  );

INSERT INTO rol_modulo_permisos (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.codigo = 'ADMINISTRADOR'
  AND m.codigo = 'TAREAS'
  AND p.codigo = 'CREAR'
  AND NOT EXISTS (
    SELECT 1
    FROM rol_modulo_permisos rmp
    WHERE rmp.rol_id = r.id
      AND rmp.modulo_id = m.id
      AND rmp.permiso_id = p.id
  );
