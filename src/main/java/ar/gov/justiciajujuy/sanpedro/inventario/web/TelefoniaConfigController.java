package ar.gov.justiciajujuy.sanpedro.inventario.web;

import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.TelefoniaConfigurationService;
import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.TelefoniaConfigurationService.SaveTelefoniaConfig;
import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.TelefoniaRuntimeConfig;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import ar.gov.justiciajujuy.sanpedro.inventario.telefonia.UcmApiClient;
import ar.gov.justiciajujuy.sanpedro.inventario.telefonia.UcmApiClient.TestResult;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class TelefoniaConfigController {

	private static final String MODULO_SISTEMA = "SISTEMA";
	private static final String PERMISO_ADMINISTRAR = "ADMINISTRAR";

	private final AuthorizationService authorizationService;
	private final TelefoniaConfigurationService telefoniaConfigurationService;
	private final UcmApiClient ucmApiClient;

	public TelefoniaConfigController(
			AuthorizationService authorizationService,
			TelefoniaConfigurationService telefoniaConfigurationService,
			UcmApiClient ucmApiClient) {
		this.authorizationService = authorizationService;
		this.telefoniaConfigurationService = telefoniaConfigurationService;
		this.ucmApiClient = ucmApiClient;
	}

	@GetMapping("/admin/sistema/telefonia")
	public String centralTelefonica(Model model, @AuthenticationPrincipal UserDetails userDetails) {
		requireAdmin(userDetails);
		addModel(model, Form.from(telefoniaConfigurationService.current()), null, null);
		return "admin/sistema-telefonia";
	}

	@PostMapping("/admin/sistema/telefonia/probar")
	public String probar(
			@ModelAttribute Form form,
			Model model,
			@AuthenticationPrincipal UserDetails userDetails) {
		requireAdmin(userDetails);
		TelefoniaRuntimeConfig preview = telefoniaConfigurationService.preview(toCommandWithExistingPassword(form));
		TestResult result = ucmApiClient.probarConexion(preview);
		addModel(model, form, result.ok() ? result.message() : null, result.ok() ? null : result.message());
		return "admin/sistema-telefonia";
	}

	@PostMapping("/admin/sistema/telefonia")
	public String guardar(
			@ModelAttribute Form form,
			Model model,
			@AuthenticationPrincipal UserDetails userDetails) {
		requireAdmin(userDetails);
		SaveTelefoniaConfig command = toCommandWithExistingPassword(form);
		TestResult result = null;
		if (command.enabled()) {
			result = ucmApiClient.probarConexion(telefoniaConfigurationService.preview(command));
		}
		telefoniaConfigurationService.save(command);

		String successMsg;
		String errorMsg = null;
		if (result != null && result.ok()) {
			successMsg = "Configuración de la Central Telefónica guardada y verificada exitosamente.";
		} else if (result != null && !result.ok()) {
			successMsg = "Configuración guardada en la base de datos.";
			errorMsg = "Aviso: No se pudo verificar la conexión con la central en este momento: " + result.message();
		} else {
			successMsg = "Configuración guardada exitosamente (integración deshabilitada).";
		}

		addModel(model, Form.from(telefoniaConfigurationService.current()), successMsg, errorMsg);
		return "admin/sistema-telefonia";
	}

	private void addModel(Model model, Form form, String success, String error) {
		TelefoniaRuntimeConfig current = telefoniaConfigurationService.current();
		model.addAttribute("form", form);
		model.addAttribute("telefoniaActual", current);
		model.addAttribute("hasSavedPassword", StringUtils.hasText(current.password()));
		model.addAttribute("success", success);
		model.addAttribute("error", error);
	}

	private SaveTelefoniaConfig toCommandWithExistingPassword(Form form) {
		String password = StringUtils.hasText(form.password())
				? form.password()
				: telefoniaConfigurationService.current().password();
		return new SaveTelefoniaConfig(
				form.enabled(),
				form.url(),
				form.user(),
				password,
				form.interno(),
				form.webhookIps(),
				form.sslStrict());
	}

	private void requireAdmin(UserDetails userDetails) {
		if (userDetails == null || !authorizationService.tienePermiso(userDetails, MODULO_SISTEMA, PERMISO_ADMINISTRAR)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
	}

	public record Form(
			boolean enabled,
			String url,
			String user,
			String password,
			String interno,
			String webhookIps,
			boolean sslStrict) {

		public static Form from(TelefoniaRuntimeConfig config) {
			return new Form(
					config.enabled(),
					config.url(),
					config.user(),
					"",
					config.interno(),
					config.webhookIps(),
					config.sslStrict());
		}
	}
}
