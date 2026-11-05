package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Servicio de control de emergencia (Kill Switch / Modo Lockdown).
 * <p>
 * Cuando está activo:
 * - Restringe el acceso a la aplicación únicamente a usuarios administradores.
 * - Bloquea operaciones mutables de usuarios no administradores y visitantes.
 * - Brinda protección inmediata ante sospecha de intrusión o anomalías en la red interna.
 */
@Service
public class LockdownService {

	private volatile boolean active = false;
	private volatile String activatedBy = null;
	private volatile Instant activatedAt = null;
	private volatile String reason = null;

	public boolean isActive() {
		return active;
	}

	public String getActivatedBy() {
		return activatedBy;
	}

	public Instant getActivatedAt() {
		return activatedAt;
	}

	public String getReason() {
		return reason;
	}

	public synchronized void activate(String adminUser, String reason) {
		this.active = true;
		this.activatedBy = adminUser != null ? adminUser.trim() : "Administrador";
		this.activatedAt = Instant.now();
		this.reason = (reason != null && !reason.isBlank()) ? reason.trim() : "Activado manualmente por seguridad.";
	}

	public synchronized void deactivate(String adminUser) {
		this.active = false;
		this.activatedBy = null;
		this.activatedAt = null;
		this.reason = null;
	}

	public Map<String, Object> estado() {
		return Map.of(
				"active", active,
				"activatedBy", activatedBy != null ? activatedBy : "",
				"activatedAt", activatedAt != null ? activatedAt.toString() : "",
				"reason", reason != null ? reason : ""
		);
	}
}
