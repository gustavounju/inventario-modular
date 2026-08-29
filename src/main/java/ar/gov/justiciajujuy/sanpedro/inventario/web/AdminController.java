package ar.gov.justiciajujuy.sanpedro.inventario.web;

import java.util.List;
import java.util.Map;

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

	private final String applicationName;
	private final String version;
	private final AuthorizationService authorizationService;

	public AdminController(
			@Value("${spring.application.name}") String applicationName,
			@Value("${inventario.version}") String version,
			AuthorizationService authorizationService) {
		this.applicationName = applicationName;
		this.version = version;
		this.authorizationService = authorizationService;
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
		model.addAttribute("adAttributes", activeDirectoryAttributes(userDetails));
		return "admin/index";
	}

	private Map<String, List<String>> activeDirectoryAttributes(UserDetails userDetails) {
		if (userDetails instanceof ActiveDirectoryUserDetails activeDirectoryUser) {
			return activeDirectoryUser.getAttributes();
		}
		return Map.of();
	}
}
