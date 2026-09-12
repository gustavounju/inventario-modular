CREATE TABLE IF NOT EXISTS tareas_stock_usos (
  id BIGINT NOT NULL AUTO_INCREMENT,
  tarea_id BIGINT NOT NULL,
  stock_componente_id BIGINT NOT NULL,
  registrado_por VARCHAR(120) NOT NULL,
  observacion VARCHAR(500) NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_tareas_stock_usos_tarea FOREIGN KEY (tarea_id) REFERENCES tareas_tecnicas (id),
  CONSTRAINT fk_tareas_stock_usos_stock FOREIGN KEY (stock_componente_id) REFERENCES stock_componentes (id),
  UNIQUE KEY uk_tareas_stock_usos_stock (stock_componente_id),
  INDEX idx_tareas_stock_usos_tarea (tarea_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
