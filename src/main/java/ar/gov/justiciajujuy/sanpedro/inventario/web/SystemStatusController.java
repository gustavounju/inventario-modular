package ar.gov.justiciajujuy.sanpedro.inventario.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sistema")
public class SystemStatusController {

	private final String applicationName;
	private final String version;
	private final Environment environment;

	public SystemStatusController(
			@Value("${spring.application.name}") String applicationName,
			@Value("${inventario.version}") String version,
			Environment environment) {
		this.applicationName = applicationName;
		this.version = version;
		this.environment = environment;
	}

	@GetMapping("/estado")
	public SystemStatusResponse status() {
		String[] activeProfiles = environment.getActiveProfiles();
		String profile = activeProfiles.length == 0 ? "default" : activeProfiles[0];
		return new SystemStatusResponse("OPERATIVO", applicationName, version, profile);
	}

	public record SystemStatusResponse(
			String estado,
			String aplicacion,
			String version,
			String perfil) {
	}
}
