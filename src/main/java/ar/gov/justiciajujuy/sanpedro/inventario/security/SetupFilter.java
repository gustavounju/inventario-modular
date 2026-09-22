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

	private final UsuarioSistemaRepository usuarioRepository;

	public SetupFilter(UsuarioSistemaRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getRequestURI();
		
		// Bypass static resources and the setup page itself
		if (path.startsWith(request.getContextPath() + "/css/") ||
			path.startsWith(request.getContextPath() + "/js/") ||
			path.startsWith(request.getContextPath() + "/images/") ||
			path.startsWith(request.getContextPath() + "/webjars/") ||
			path.equals(request.getContextPath() + "/favicon.ico") ||
			path.equals(request.getContextPath() + "/setup")) {
			
			filterChain.doFilter(request, response);
			return;
		}

		// If there are zero admins in the system, intercept and redirect to /setup
		if (usuarioRepository.countAdministradores() == 0) {
			response.sendRedirect(request.getContextPath() + "/setup");
			return;
		}

		filterChain.doFilter(request, response);
	}
}
