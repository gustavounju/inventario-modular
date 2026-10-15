package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SoloMovilAccessInterceptor implements HandlerInterceptor {

	private final AuthorizationService authorizationService;

	public SoloMovilAccessInterceptor(AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
		String uri = request.getRequestURI();
		String contextPath = request.getContextPath();
		String relativeUri = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;

		// El visor público de tareas en vivo debe poder verse en pantallas grandes / talleres
		if (relativeUri.startsWith("/admin/tareas/visor")) {
			return true;
		}

		// Solo redirigir navegacion visual (GET). Peticiones POST/PUT/DELETE deben seguir su flujo normal de seguridad o rechazo (403).
		if (!"GET".equalsIgnoreCase(request.getMethod())) {
			return true;
		}

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails userDetails) {
			if (authorizationService.esSoloMovil(userDetails)) {
				if (relativeUri.startsWith("/admin/stock")) {
					response.sendRedirect(contextPath + "/movil/stock");
				} else {
					response.sendRedirect(contextPath + "/movil/tareas");
				}
				return false;
			}
		}
		return true;
	}
}
