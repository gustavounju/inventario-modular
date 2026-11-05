package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

public class LockdownFilter extends OncePerRequestFilter {

	private final LockdownService lockdownService;
	private final AuthorizationService authorizationService;

	public LockdownFilter(LockdownService lockdownService, AuthorizationService authorizationService) {
		this.lockdownService = lockdownService;
		this.authorizationService = authorizationService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		if (!lockdownService.isActive()) {
			filterChain.doFilter(request, response);
			return;
		}

		String uri = request.getRequestURI();
		if (isPermittedPublicPath(uri)) {
			filterChain.doFilter(request, response);
			return;
		}

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails userDetails) {
			if (authorizationService.esAdministrador(userDetails)) {
				// El administrador tiene paso libre para investigar, auditar y desactivar el bloqueo
				filterChain.doFilter(request, response);
				return;
			}
		}

		// Si es una petición API, devolver respuesta JSON 423 (Locked)
		if (uri.startsWith("/api/") || uri.equals("/submit_inventory")) {
			response.setStatus(423); // 423 Locked
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("""
					{"ok":false,"lockdown":true,"mensaje":"SISTEMA EN MODO LOCKDOWN: Acceso suspendido preventivamente por el Administrador de Seguridad Judicial."}
					""");
			return;
		}

		// Si es una página web, redirigir al login con mensaje de bloqueo de contingencia
		response.setStatus(423);
		response.sendRedirect(request.getContextPath() + "/login?lockdown");
	}

	private boolean isPermittedPublicPath(String uri) {
		return uri.endsWith("/login")
				|| uri.endsWith("/logout")
				|| uri.endsWith("/setup")
				|| uri.startsWith("/css/")
				|| uri.startsWith("/js/")
				|| uri.startsWith("/images/")
				|| uri.startsWith("/scripts/")
				|| uri.startsWith("/webjars/")
				|| uri.equals("/favicon.ico")
				|| uri.equals("/error");
	}
}

