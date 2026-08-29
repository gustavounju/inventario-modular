MERGE INTO permisos (id, codigo, nombre, descripcion) KEY(codigo) VALUES
  (1, 'VER', 'Ver', 'Permite consultar informacion del modulo.'),
  (2, 'CREAR', 'Crear', 'Permite crear registros nuevos.'),
  (3, 'EDITAR', 'Editar', 'Permite modificar registros existentes.'),
  (4, 'ELIMINAR', 'Eliminar', 'Permite eliminar o desactivar registros.'),
  (5, 'EXPORTAR', 'Exportar', 'Permite generar salidas o reportes del modulo.'),
  (6, 'ADMINISTRAR', 'Administrar', 'Permite administrar configuracion del modulo.');

MERGE INTO modulos (id, codigo, nombre, descripcion, orden, activo) KEY(codigo) VALUES
  (1, 'EQUIPOS', 'Equipos', 'Inventario tecnico de computadoras y dispositivos principales.', 10, TRUE),
  (2, 'ACTAS', 'Actas', 'Gestion de actas y constancias asociadas al inventario.', 20, TRUE),
  (3, 'MUEBLES', 'Muebles', 'Gestion fisica de muebles y bienes de oficina.', 30, TRUE),
  (4, 'PATRIMONIO', 'Patrimonio', 'Control patrimonial institucional y reportes administrativos.', 40, TRUE),
  (5, 'STOCK', 'Stock', 'Existencias, movimientos y disponibilidad de insumos.', 50, TRUE),
  (6, 'COMPONENTES', 'Componentes', 'Partes, repuestos y componentes asociados a equipos.', 60, TRUE),
  (7, 'USUARIOS', 'Usuarios', 'Administracion de usuarios, roles, permisos y modulos.', 70, TRUE),
  (8, 'REPORTES', 'Reportes', 'Consultas, listados y exportaciones del sistema.', 80, TRUE),
  (9, 'TAREAS', 'Tareas', 'Seguimiento de tareas tecnicas y operativas.', 90, TRUE);

MERGE INTO roles (id, codigo, nombre, descripcion, activo) KEY(codigo) VALUES
  (1, 'ADMINISTRADOR', 'Administrador', 'Acceso total a los modulos del sistema.', TRUE),
  (2, 'TECNICO', 'Tecnico', 'Acceso operativo a modulos tecnicos.', TRUE),
  (3, 'PATRIMONIO', 'Patrimonio', 'Acceso a gestion patrimonial, muebles y reportes.', TRUE),
  (4, 'LECTOR', 'Lector', 'Acceso de solo consulta.', TRUE),
  (5, 'PERSONALIZADO', 'Personalizado', 'Rol para combinaciones manuales de permisos.', TRUE);

MERGE INTO usuarios (id, username, nombre_visible, fuero, activo) KEY(username) VALUES
  (1, 'admin.local', 'Administrador Local', 'Desarrollo local', TRUE);

MERGE INTO usuario_roles (usuario_id, rol_id) KEY(usuario_id, rol_id)
SELECT u.id, r.id
FROM usuarios u
JOIN roles r ON r.codigo = 'ADMINISTRADOR'
WHERE u.username = 'admin.local';

MERGE INTO rol_modulo_permisos (rol_id, modulo_id, permiso_id) KEY(rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.codigo = 'ADMINISTRADOR';
