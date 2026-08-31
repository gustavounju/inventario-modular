package ar.gov.justiciajujuy.sanpedro.inventario.web;

import java.util.List;
import java.util.Map;

import ar.gov.justiciajujuy.sanpedro.inventario.config.RuntimeModeService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryUserDetails;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService.UsuarioActual;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

	private static final String MODULO_USUARIOS = "USUARIOS";
	private static final String MODULO_EQUIPOS = "EQUIPOS";
	private static final String PERMISO_ADMINISTRAR = "ADMINISTRAR";
	private static final String PERMISO_VER = "VER";

	private final String applicationName;
	private final String version;
	private final AuthorizationService authorizationService;
	private final RuntimeModeService runtimeModeService;

	public AdminController(
			@Value("${spring.application.name}") String applicationName,
			@Value("${inventario.version}") String version,
			AuthorizationService authorizationService,
			RuntimeModeService runtimeModeService) {
		this.applicationName = applicationName;
		this.version = version;
		this.authorizationService = authorizationService;
		this.runtimeModeService = runtimeModeService;
	}

	@GetMapping("/")
	public String index() {
		return "redirect:/admin";
	}

	@GetMapping("/admin")
	public String admin(Model model, @AuthenticationPrincipal UserDetails userDetails) {
		UsuarioActual usuarioActual = authorizationService.obtenerUsuarioActual(userDetails);
		model.addAttribute("applicationName", applicationName);
		model.addAttribute("version", version);
		model.addAttribute("username", usuarioActual.username());
		model.addAttribute("displayName", usuarioActual.nombreVisible());
		model.addAttribute("fuero", usuarioActual.fuero());
		model.addAttribute("authorized", usuarioActual.autorizado());
		model.addAttribute("modules", usuarioActual.modulos());
		model.addAttribute("canViewEquipos",
				authorizationService.tienePermiso(userDetails, MODULO_EQUIPOS, PERMISO_VER));
		model.addAttribute("canManageUsers",
				authorizationService.tienePermiso(userDetails, MODULO_USUARIOS, PERMISO_ADMINISTRAR));
		model.addAttribute("adAttributes", activeDirectoryAttributes(userDetails));
		model.addAttribute("runtimeMode", runtimeModeService.current());
		return "admin/index";
	}

	private Map<String, List<String>> activeDirectoryAttributes(UserDetails userDetails) {
		if (userDetails instanceof ActiveDirectoryUserDetails activeDirectoryUser) {
			return activeDirectoryUser.getAttributes();
		}
		return Map.of();
	}
}
