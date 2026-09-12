ALTER TABLE stock_componentes
  ADD COLUMN datos_completos BOOLEAN NOT NULL DEFAULT TRUE AFTER ingresado_por;
