DELETE FROM credenciales_locales WHERE usuario_id IN (SELECT id FROM usuarios WHERE username = 'admin.local');
DELETE FROM usuario_roles WHERE usuario_id IN (SELECT id FROM usuarios WHERE username = 'admin.local');
DELETE FROM usuarios WHERE username = 'admin.local';
