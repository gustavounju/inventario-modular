package ar.gov.justiciajujuy.sanpedro.inventario.web;

import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryUserDetails;
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

	public AdminController(
			@Value("${spring.application.name}") String applicationName,
			@Value("${inventario.version}") String version) {
		this.applicationName = applicationName;
		this.version = version;
	}

	@GetMapping("/")
	public String index() {
		return "redirect:/admin";
	}

	@GetMapping("/admin")
	public String admin(Model model, @AuthenticationPrincipal UserDetails userDetails) {
		model.addAttribute("applicationName", applicationName);
		model.addAttribute("version", version);
		model.addAttribute("username", username(userDetails));
		model.addAttribute("displayName", displayName(userDetails));
		model.addAttribute("fuero", fuero(userDetails));
		return "admin/index";
	}

	private String username(UserDetails userDetails) {
		return userDetails != null ? userDetails.getUsername() : "usuario";
	}

	private String displayName(UserDetails userDetails) {
		if (userDetails instanceof ActiveDirectoryUserDetails activeDirectoryUser) {
			return activeDirectoryUser.getDisplayName();
		}
		return username(userDetails);
	}

	private String fuero(UserDetails userDetails) {
		if (userDetails instanceof ActiveDirectoryUserDetails activeDirectoryUser) {
			return activeDirectoryUser.getFuero();
		}
		return "Sin fuero informado";
	}
}
