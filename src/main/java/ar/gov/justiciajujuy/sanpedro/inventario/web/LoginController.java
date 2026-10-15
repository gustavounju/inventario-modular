package ar.gov.justiciajujuy.sanpedro.inventario.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

	private final String applicationName;
	private final String reportToken;

	public LoginController(
			@Value("${spring.application.name}") String applicationName,
			@Value("${inventario.security.report-token:}") String reportToken) {
		this.applicationName = applicationName;
		this.reportToken = reportToken;
	}

	@GetMapping("/login")
	public String login(
			@RequestParam(value = "error", required = false) String error,
			@RequestParam(value = "logout", required = false) String logout,
			@RequestParam(value = "bloqueado", required = false) String bloqueado,
			Model model) {
		model.addAttribute("applicationName", applicationName);
		model.addAttribute("reportToken", reportToken);
		model.addAttribute("hasError", error != null);
		model.addAttribute("loggedOut", logout != null);
		model.addAttribute("blocked", bloqueado != null);
		return "login";
	}

	@GetMapping(value = "/app/inventario-modular.url", produces = "application/octet-stream")
	@org.springframework.web.bind.annotation.ResponseBody
	public org.springframework.http.ResponseEntity<byte[]> descargarAccesoDirecto(jakarta.servlet.http.HttpServletRequest request) {
		String scheme = request.getScheme();
		String serverName = request.getServerName();
		int serverPort = request.getServerPort();
		String portPart = (("http".equalsIgnoreCase(scheme) && serverPort == 80) || ("https".equalsIgnoreCase(scheme) && serverPort == 443))
				? ""
				: ":" + serverPort;
		String baseUrl = scheme + "://" + serverName + portPart + request.getContextPath() + "/";
		String shortcutContent = "[InternetShortcut]\r\nURL=" + baseUrl + "\r\nIconIndex=0\r\n";
		byte[] bytes = shortcutContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		return org.springframework.http.ResponseEntity.ok()
				.header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Inventario-Modular.url\"")
				.header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/octet-stream")
				.body(bytes);
	}
}
