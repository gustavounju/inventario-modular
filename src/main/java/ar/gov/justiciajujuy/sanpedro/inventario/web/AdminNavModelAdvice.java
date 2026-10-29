package ar.gov.justiciajujuy.sanpedro.inventario.web;

import ar.gov.justiciajujuy.sanpedro.inventario.config.RuntimeModeService;
import ar.gov.justiciajujuy.sanpedro.inventario.config.RuntimeModeService.RuntimeMode;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService.UsuarioActual;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AdminNavModelAdvice {

	private final AuthorizationService authorizationService;
	private final RuntimeModeService runtimeModeService;

	@Autowired
	public AdminNavModelAdvice(
			AuthorizationService authorizationService,
			@Autowired(required = false) RuntimeModeService runtimeModeService) {
		this.authorizationService = authorizationService;
		this.runtimeModeService = runtimeModeService;
	}

	public AdminNavModelAdvice(AuthorizationService authorizationService) {
		this(authorizationService, null);
	}

	private boolean isApiRequest(HttpServletRequest request) {
		if (request == null) {
			return false;
		}
		String contextPath = request.getContextPath() != null ? request.getContextPath() : "";
		String uri = request.getRequestURI();
		return uri != null && (uri.startsWith(contextPath + "/api/")
				|| uri.equals(contextPath + "/submit_inventory")
				|| uri.contains("/api/"));
	}

	@ModelAttribute("runtimeMode")
	public RuntimeMode runtimeMode(HttpServletRequest request) {
		if (isApiRequest(request) || runtimeModeService == null) {
			return null;
		}
		return runtimeModeService.current();
	}

	@ModelAttribute("currentUser")
	public UsuarioActual currentUser(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		if (userDetails == null || isApiRequest(request)) {
			return null;
		}
		return authorizationService.obtenerUsuarioActual(userDetails);
	}

	@ModelAttribute("displayName")
	public String displayName(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		if (userDetails == null || isApiRequest(request)) {
			return "";
		}
		UsuarioActual u = authorizationService.obtenerUsuarioActual(userDetails);
		return u != null ? u.nombreVisible() : "";
	}

	@ModelAttribute("username")
	public String username(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		if (userDetails == null || isApiRequest(request)) {
			return "";
		}
		return userDetails.getUsername();
	}

	@ModelAttribute("fuero")
	public String fuero(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		if (userDetails == null || isApiRequest(request)) {
			return "";
		}
		UsuarioActual u = authorizationService.obtenerUsuarioActual(userDetails);
		return u != null ? u.fuero() : "";
	}

	@ModelAttribute("canViewEquipos")
	public boolean canViewEquipos(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		return userDetails != null && !isApiRequest(request) && authorizationService.tienePermiso(userDetails, "EQUIPOS", "VER");
	}

	@ModelAttribute("canViewDashboardDiferencias")
	public boolean canViewDashboardDiferencias(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		return userDetails != null && !isApiRequest(request) && authorizationService.tienePermiso(userDetails, "COMPONENTES", "VER");
	}

	@ModelAttribute("canViewStock")
	public boolean canViewStock(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		return userDetails != null && !isApiRequest(request) && authorizationService.tienePermiso(userDetails, "STOCK", "VER");
	}

	@ModelAttribute("canViewOrdenesArmado")
	public boolean canViewOrdenesArmado(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		return userDetails != null && !isApiRequest(request) && authorizationService.tienePermiso(userDetails, "ORDENES_ARMADO", "VER");
	}

	@ModelAttribute("canViewMuebles")
	public boolean canViewMuebles(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		return userDetails != null && !isApiRequest(request) && authorizationService.tienePermiso(userDetails, "MUEBLES", "VER");
	}

	@ModelAttribute("canViewUbicaciones")
	public boolean canViewUbicaciones(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		return userDetails != null && !isApiRequest(request) && authorizationService.tienePermiso(userDetails, "UBICACIONES", "VER");
	}

	@ModelAttribute("canViewActas")
	public boolean canViewActas(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		return userDetails != null && !isApiRequest(request) && authorizationService.tienePermiso(userDetails, "ACTAS", "VER");
	}

	@ModelAttribute("canViewReportes")
	public boolean canViewReportes(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		return userDetails != null && !isApiRequest(request) && authorizationService.tienePermiso(userDetails, "REPORTES", "VER");
	}

	@ModelAttribute("canViewAuditoria")
	public boolean canViewAuditoria(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		return userDetails != null && !isApiRequest(request) && authorizationService.tienePermiso(userDetails, "AUDITORIA", "VER");
	}

	@ModelAttribute("canViewTareas")
	public boolean canViewTareas(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		return userDetails != null && !isApiRequest(request) && authorizationService.tienePermiso(userDetails, "TAREAS", "VER");
	}

	@ModelAttribute("canManageUsers")
	public boolean canManageUsers(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		return userDetails != null && !isApiRequest(request) && authorizationService.tienePermiso(userDetails, "USUARIOS", "ADMINISTRAR");
	}

	@ModelAttribute("canManageSystem")
	public boolean canManageSystem(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
		return userDetails != null && !isApiRequest(request) && authorizationService.tienePermiso(userDetails, "SISTEMA", "ADMINISTRAR");
	}
}
