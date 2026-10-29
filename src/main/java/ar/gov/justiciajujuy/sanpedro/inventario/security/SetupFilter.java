package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de seguridad que intercepta todas las peticiones HTTP al servidor.
 * Su objetivo es verificar si existe al menos un usuario Administrador en el sistema.
 * Si no existe (por ejemplo, en el primer inicio de la aplicación o si fue borrado),
 * redirige obligatoriamente a la pantalla de configuración inicial (/setup) 
 * impidiendo el acceso a cualquier otra funcionalidad hasta que se cree un administrador.
 */
@Component
public class SetupFilter extends OncePerRequestFilter {

	private static final long SETUP_CACHE_TTL_MS = 30_000L;

	private final UsuarioSistemaRepository usuarioRepository;
	private volatile long setupVerifiedUntilMs = 0L;

	public SetupFilter(UsuarioSistemaRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	public void invalidateCache() {
		this.setupVerifiedUntilMs = 0L;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String contextPath = request.getContextPath() != null ? request.getContextPath() : "";
		String path = request.getRequestURI();

		// Bypass static resources, public scripts, downloads and the setup page itself
		if (path.startsWith(contextPath + "/css/") ||
			path.startsWith(contextPath + "/js/") ||
			path.startsWith(contextPath + "/images/") ||
			path.startsWith(contextPath + "/webjars/") ||
			path.startsWith(contextPath + "/scripts/") ||
			path.startsWith(contextPath + "/app/") ||
			path.equals(contextPath + "/favicon.ico") ||
			path.equals(contextPath + "/setup")) {

			filterChain.doFilter(request, response);
			return;
		}

		long now = System.currentTimeMillis();
		if (now < setupVerifiedUntilMs) {
			filterChain.doFilter(request, response);
			return;
		}

		// If there are zero admins in the system, intercept and redirect to /setup
		try {
			if (usuarioRepository.countAdministradores() == 0) {
				setupVerifiedUntilMs = 0L;
				response.sendRedirect(contextPath + "/setup");
				return;
			}
			setupVerifiedUntilMs = now + SETUP_CACHE_TTL_MS;
		} catch (Exception ignored) {
			// Si la base no contiene la tabla o se encuentra en inicialización, continuar la cadena
		}

		filterChain.doFilter(request, response);
	}
}
