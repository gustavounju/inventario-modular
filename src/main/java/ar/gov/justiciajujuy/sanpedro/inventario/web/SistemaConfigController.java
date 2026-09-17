package ar.gov.justiciajujuy.sanpedro.inventario.web;

import java.util.List;

import javax.naming.directory.SearchControls;

import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.LdapConfigurationService;
import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.LdapConfigurationService.SaveLdapConfig;
import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.LdapRuntimeConfig;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.RuntimeLdapClientFactory;
import ar.gov.justiciajujuy.sanpedro.inventario.security.UsuarioManagementService;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.ldap.core.LdapOperations;

@Controller
public class SistemaConfigController {

	private static final String MODULO_SISTEMA = "SISTEMA";
	private static final String PERMISO_ADMINISTRAR = "ADMINISTRAR";

	private final AuthorizationService authorizationService;
	private final LdapConfigurationService ldapConfigurationService;
	private final RuntimeLdapClientFactory runtimeLdapClientFactory;
	private final UsuarioManagementService usuarioManagementService;

	public SistemaConfigController(
			AuthorizationService authorizationService,
			LdapConfigurationService ldapConfigurationService,
			RuntimeLdapClientFactory runtimeLdapClientFactory,
			UsuarioManagementService usuarioManagementService) {
		this.authorizationService = authorizationService;
		this.ldapConfigurationService = ldapConfigurationService;
		this.runtimeLdapClientFactory = runtimeLdapClientFactory;
		this.usuarioManagementService = usuarioManagementService;
	}

	@GetMapping("/admin/sistema/ad")
	public String activeDirectory(Model model, @AuthenticationPrincipal UserDetails userDetails) {
		requireAdmin(userDetails);
		addModel(model, Form.from(ldapConfigurationService.current()), null, null);
		return "admin/sistema-ad";
	}

	@PostMapping("/admin/sistema/ad/probar")
	public String probar(
			@ModelAttribute Form form,
			Model model,
			@AuthenticationPrincipal UserDetails userDetails) {
		requireAdmin(userDetails);
		TestResult result = test(ldapConfigurationService.preview(toCommandWithExistingPassword(form)));
		addModel(model, form, result.ok() ? result.message() : null, result.ok() ? null : result.message());
		return "admin/sistema-ad";
	}

	@PostMapping("/admin/sistema/ad")
	public String guardar(
			@ModelAttribute Form form,
			Model model,
			@AuthenticationPrincipal UserDetails userDetails) {
		requireAdmin(userDetails);
		SaveLdapConfig command = toCommandWithExistingPassword(form);
		TestResult result = test(ldapConfigurationService.preview(command));
		if (!result.ok()) {
			addModel(model, form, null, "No se guardo: " + result.message());
			return "admin/sistema-ad";
		}
		ldapConfigurationService.save(command);
		String bootstrapUsername = usernameFromDnOrLogin(command.readOnlyUserDn());
		// El usuario lector probado queda autorizado como administrador para evitar bloqueo al apagar admin.local.
		usuarioManagementService.asegurarAdministradorDominio(
				bootstrapUsername,
				bootstrapUsername,
				"Administrador Active Directory");
		addModel(model, Form.from(ldapConfigurationService.current()),
				"Configuracion Active Directory guardada. " + bootstrapUsername + " quedo autorizado como administrador AD; admin.local queda deshabilitado desde el proximo ingreso.",
				null);
		return "admin/sistema-ad";
	}

	private void addModel(Model model, Form form, String success, String error) {
		LdapRuntimeConfig current = ldapConfigurationService.current();
		model.addAttribute("form", form);
		model.addAttribute("ldapActual", current);
		model.addAttribute("hasSavedPassword", StringUtils.hasText(current.readOnlyPassword()));
		model.addAttribute("success", success);
		model.addAttribute("error", error);
	}

	private SaveLdapConfig toCommandWithExistingPassword(Form form) {
		String password = StringUtils.hasText(form.readOnlyPassword())
				? form.readOnlyPassword()
				: ldapConfigurationService.current().readOnlyPassword();
		return new SaveLdapConfig(
				form.enabled(),
				form.url(),
				form.domain(),
				form.baseDn(),
				form.displayNameAttribute(),
				form.fueroAttribute(),
				form.readOnlyUserDn(),
				password,
				form.userSearchBase(),
				form.userSearchFilter(),
				form.userSearchLimit());
	}

	private TestResult test(LdapRuntimeConfig config) {
		if (!config.enabled()) {
			return new TestResult(false, "Active Directory esta deshabilitado en el formulario.");
		}
		if (!StringUtils.hasText(config.readOnlyUserDn()) || !StringUtils.hasText(config.readOnlyPassword())) {
			return new TestResult(false, "Debe cargar usuario lector y clave para probar la conexion.");
		}
		try {
			LdapOperations operations = runtimeLdapClientFactory.createOperations(config);
			SearchControls controls = new SearchControls();
			controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			controls.setCountLimit(1);
			controls.setReturningAttributes(new String[] { "sAMAccountName" });
			List<String> results = operations.search(
					config.userSearchBase(),
					config.userSearchFilter(),
					controls,
					(AttributesMapper<String>) attrs -> String.valueOf(attrs.get("sAMAccountName").get()));
			return new TestResult(true, "Conexion LDAP correcta. Usuarios encontrados en la base configurada: " + results.size() + ".");
		} catch (RuntimeException exception) {
			return new TestResult(false, "No se pudo conectar o buscar en AD: " + exception.getMessage());
		}
	}

	private void requireAdmin(UserDetails userDetails) {
		if (userDetails == null || !authorizationService.tienePermiso(userDetails, MODULO_SISTEMA, PERMISO_ADMINISTRAR)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
	}

	private String usernameFromDnOrLogin(String value) {
		String clean = value == null ? "" : value.trim();
		int slash = clean.indexOf('\\');
		if (slash >= 0 && slash + 1 < clean.length()) {
			return clean.substring(slash + 1).trim().toLowerCase();
		}
		int at = clean.indexOf('@');
		if (at > 0) {
			return clean.substring(0, at).trim().toLowerCase();
		}
		if (clean.regionMatches(true, 0, "CN=", 0, 3)) {
			int comma = clean.indexOf(',');
			return clean.substring(3, comma > 3 ? comma : clean.length()).trim().toLowerCase();
		}
		return clean.toLowerCase();
	}

	private record TestResult(boolean ok, String message) {
	}

	public record Form(
			boolean enabled,
			String url,
			String domain,
			String baseDn,
			String displayNameAttribute,
			String fueroAttribute,
			String readOnlyUserDn,
			String readOnlyPassword,
			String userSearchBase,
			String userSearchFilter,
			int userSearchLimit) {

		static Form from(LdapRuntimeConfig config) {
			return new Form(
					config.enabled(),
					config.url(),
					config.domain(),
					config.baseDn(),
					config.displayNameAttribute(),
					config.fueroAttribute(),
					config.readOnlyUserDn(),
					"",
					config.userSearchBase(),
					config.userSearchFilter(),
					config.userSearchLimit());
		}
	}
}
