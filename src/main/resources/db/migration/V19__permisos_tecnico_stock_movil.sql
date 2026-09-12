INSERT INTO rol_modulo_permisos (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.codigo = 'TECNICO'
  AND m.codigo = 'STOCK'
  AND p.codigo IN ('VER', 'EDITAR')
  AND NOT EXISTS (
    SELECT 1
    FROM rol_modulo_permisos rmp
    WHERE rmp.rol_id = r.id
      AND rmp.modulo_id = m.id
      AND rmp.permiso_id = p.id
  );
