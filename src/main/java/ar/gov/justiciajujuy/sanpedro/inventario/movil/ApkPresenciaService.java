package ar.gov.justiciajujuy.sanpedro.inventario.movil;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryUserDetails;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class ApkPresenciaService {

	private static final Duration TIMEOUT = Duration.ofSeconds(35);
	private static final Pattern VERSION_PATTERN = Pattern.compile("InventarioLANVersion/([^ ]+)");

	private final Map<String, PresenciaInterna> presencias = new ConcurrentHashMap<>();
	private final AuthorizationService authorizationService;

	public ApkPresenciaService(AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}

	public void registrarActividad(UserDetails userDetails, String userAgent) {
		if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
			return;
		}
		String username = userDetails.getUsername().trim().toLowerCase();
		boolean esApk = userAgent != null && userAgent.contains("InventarioLAN");

		String version = null;
		if (userAgent != null) {
			Matcher m = VERSION_PATTERN.matcher(userAgent);
			if (m.find()) {
				version = m.group(1);
			}
		}

		var usuarioActual = authorizationService.obtenerUsuarioActual(userDetails);
		String nombreVisible = usuarioActual != null && usuarioActual.nombreVisible() != null && !usuarioActual.nombreVisible().isBlank()
				? usuarioActual.nombreVisible()
				: (userDetails instanceof ActiveDirectoryUserDetails adUser ? adUser.getDisplayName() : userDetails.getUsername());

		String fuero = usuarioActual != null && usuarioActual.fuero() != null ? usuarioActual.fuero() : "";
		String iniciales = calcularIniciales(nombreVisible, username);

		presencias.put(username, new PresenciaInterna(
				username,
				nombreVisible,
				iniciales,
				fuero,
				esApk,
				version,
				Instant.now()
		));
	}

	public List<UsuarioConectadoDto> obtenerConectados() {
		Instant ahora = Instant.now();
		Instant limite = ahora.minus(TIMEOUT);
		Instant limpieza = ahora.minus(Duration.ofHours(1));
		presencias.entrySet().removeIf(e -> e.getValue().ultimaActividad().isBefore(limpieza));

		return presencias.values().stream()
				.filter(p -> p.ultimaActividad().isAfter(limite))
				.sorted(Comparator.comparing(PresenciaInterna::ultimaActividad).reversed())
				.map(p -> new UsuarioConectadoDto(
						p.username(),
						p.nombreVisible(),
						p.iniciales(),
						p.fuero(),
						p.esApk(),
						p.version(),
						Duration.between(p.ultimaActividad(), ahora).getSeconds()
				))
				.toList();
	}

	private String calcularIniciales(String nombre, String username) {
		if (nombre != null && !nombre.isBlank()) {
			String[] partes = nombre.trim().split("\\s+");
			if (partes.length >= 2) {
				String p1 = partes[0].substring(0, Math.min(1, partes[0].length()));
				String p2 = partes[partes.length - 1].substring(0, Math.min(1, partes[partes.length - 1].length()));
				return (p1 + p2).toUpperCase();
			} else if (partes[0].length() >= 2) {
				return partes[0].substring(0, 2).toUpperCase();
			} else if (!partes[0].isEmpty()) {
				return partes[0].substring(0, 1).toUpperCase();
			}
		}
		if (username != null && username.length() >= 2) {
			return username.substring(0, 2).toUpperCase();
		}
		return "TK";
	}

	private record PresenciaInterna(
			String username,
			String nombreVisible,
			String iniciales,
			String fuero,
			boolean esApk,
			String version,
			Instant ultimaActividad
	) {}

	public record UsuarioConectadoDto(
			String username,
			String nombreVisible,
			String iniciales,
			String fuero,
			boolean esApk,
			String version,
			long segundosInactivo
	) {}
}
