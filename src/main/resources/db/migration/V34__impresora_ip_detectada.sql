-- =========================================================================
-- V34: IP detectada para impresoras reportadas por script (WSD / TCP-IP)
-- =========================================================================

ALTER TABLE equipos ADD COLUMN impresora_ip VARCHAR(45) NULL AFTER impresora;
