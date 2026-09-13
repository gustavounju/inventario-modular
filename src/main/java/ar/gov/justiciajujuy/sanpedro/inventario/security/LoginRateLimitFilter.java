package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

public class LoginRateLimitFilter extends OncePerRequestFilter {

	private final LoginAttemptService loginAttemptService;

	public LoginRateLimitFilter(LoginAttemptService loginAttemptService) {
		this.loginAttemptService = loginAttemptService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!isLoginPost(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		String username = request.getParameter("username");
		if (!loginAttemptService.isBlocked(username, request.getRemoteAddr())) {
			filterChain.doFilter(request, response);
			return;
		}

		String destino = "movil".equals(request.getParameter("destino")) ? "/movil/login?bloqueado" : "/login?bloqueado";
		response.setStatus(429);
		response.sendRedirect(request.getContextPath() + destino);
	}

	private boolean isLoginPost(HttpServletRequest request) {
		return HttpMethod.POST.matches(request.getMethod()) && request.getRequestURI().endsWith("/login");
	}
}
