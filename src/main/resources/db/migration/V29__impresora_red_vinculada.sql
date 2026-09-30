ALTER TABLE equipos
ADD COLUMN impresora_red_id BIGINT NULL,
ADD CONSTRAINT fk_equipo_impresora_red FOREIGN KEY (impresora_red_id) REFERENCES equipos (id) ON DELETE SET NULL;
