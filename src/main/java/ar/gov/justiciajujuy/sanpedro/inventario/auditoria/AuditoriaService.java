package ar.gov.justiciajujuy.sanpedro.inventario.auditoria;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuditoriaService {

	private static final int DETALLE_MAXIMO = 1000;

	private final AuditoriaEventoRepository auditoriaEventoRepository;

	public AuditoriaService(AuditoriaEventoRepository auditoriaEventoRepository) {
		this.auditoriaEventoRepository = auditoriaEventoRepository;
	}

	@Transactional(readOnly = true)
	public List<AuditoriaEventoDetalle> listarRecientes() {
		return auditoriaEventoRepository.findTop100ByOrderByIdDesc().stream()
				.map(this::toDetalle)
				.toList();
	}

	@Transactional
	public void registrar(String modulo, String accion, String entidadTipo, Long entidadId, String detalle) {
		auditoriaEventoRepository.save(new AuditoriaEvento(
				usuarioActual(),
				textoRequerido(modulo, "SISTEMA"),
				textoRequerido(accion, "CAMBIO"),
				textoRequerido(entidadTipo, "REGISTRO"),
				entidadId,
				recortar(textoRequerido(detalle, "Cambio registrado."))));
	}

	private AuditoriaEventoDetalle toDetalle(AuditoriaEvento evento) {
		return new AuditoriaEventoDetalle(
				evento.getId(),
				evento.getUsuario(),
				evento.getModulo(),
				evento.getAccion(),
				evento.getEntidadTipo(),
				evento.getEntidadId(),
				evento.getDetalle(),
				evento.getCreadoEn());
	}

	private String usuarioActual() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getPrincipal() == null) {
			return "SISTEMA";
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof UserDetails userDetails) {
			return userDetails.getUsername();
		}
		return authentication.getName();
	}

	private String textoRequerido(String valor, String fallback) {
		return StringUtils.hasText(valor) ? valor.trim() : fallback;
	}

	private String recortar(String valor) {
		return valor.length() <= DETALLE_MAXIMO ? valor : valor.substring(0, DETALLE_MAXIMO);
	}

	public record AuditoriaEventoDetalle(
			Long id,
			String usuario,
			String modulo,
			String accion,
			String entidadTipo,
			Long entidadId,
			String detalle,
			LocalDateTime creadoEn) {
	}
}
