package ar.gov.justiciajujuy.sanpedro.inventario.web;

import org.springframework.beans.factory.annotation.Value;
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
	public String admin(Model model) {
		model.addAttribute("applicationName", applicationName);
		model.addAttribute("version", version);
		return "admin/index";
	}
}
