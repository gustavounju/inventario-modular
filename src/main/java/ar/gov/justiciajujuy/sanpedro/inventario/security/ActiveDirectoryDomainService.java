package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import ar.gov.justiciajujuy.sanpedro.inventario.config.ActiveDirectoryProperties;
import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.LdapConfigurationService;
import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.LdapRuntimeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Servicio de integración con Active Directory / LDAP para consulta de usuarios y unidades
 * organizacionales (OU) del dominio del Poder Judicial.
 */
@Service
public class ActiveDirectoryDomainService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ActiveDirectoryDomainService.class);
	private static final int MIN_QUERY_LENGTH = 2;
	private static final String ATTRIBUTE_NAME_PATTERN = "[a-zA-Z][a-zA-Z0-9-]*";

	private final ActiveDirectoryProperties properties;
	private final LdapOperations ldapOperations;
	private final LdapConfigurationService ldapConfigurationService;
	private final RuntimeLdapClientFactory runtimeLdapClientFactory;

	@Autowired
	public ActiveDirectoryDomainService(
			ActiveDirectoryProperties properties,
			ObjectProvider<LdapOperations> ldapOperations,
			ObjectProvider<LdapConfigurationService> ldapConfigurationService,
			ObjectProvider<RuntimeLdapClientFactory> runtimeLdapClientFactory) {
		this(properties, ldapOperations.getIfAvailable(),
				ldapConfigurationService.getIfAvailable(),
				runtimeLdapClientFactory.getIfAvailable());
	}

	ActiveDirectoryDomainService(
			ActiveDirectoryProperties properties,
			LdapOperations ldapOperations) {
		this.properties = properties;
		this.ldapOperations = ldapOperations;
		this.ldapConfigurationService = null;
		this.runtimeLdapClientFactory = null;
	}

	ActiveDirectoryDomainService(
			ActiveDirectoryProperties properties,
			LdapOperations ldapOperations,
			LdapConfigurationService ldapConfigurationService,
			RuntimeLdapClientFactory runtimeLdapClientFactory) {
		this.properties = properties;
		this.ldapOperations = ldapOperations;
		this.ldapConfigurationService = ldapConfigurationService;
		this.runtimeLdapClientFactory = runtimeLdapClientFactory;
	}

	public DominioUsuarios listarUsuarios() {
		return DominioUsuarios.esperandoBusqueda(
				"Ingrese al menos " + MIN_QUERY_LENGTH + " caracteres para buscar usuarios de dominio.");
	}

	public DominioUsuarios listarUsuariosParaTareas() {
		LdapRuntimeConfig config = currentConfig();
		LdapOperations operations = currentOperations(config);
		if (!config.enabled()) {
			return DominioUsuarios.noDisponible("LDAP esta desactivado en este entorno.", "");
		}

		if (operations == null) {
			return DominioUsuarios.noDisponible("No hay cliente LDAP de lectura configurado.", "");
		}

		if (StringUtils.hasText(config.readOnlyUserDn())
				&& !StringUtils.hasText(config.readOnlyPassword())) {
			return DominioUsuarios.noDisponible("La cuenta LDAP lectora no tiene clave configurada.", "");
		}

		try {
			String displayNameAttribute = safeAttributeName(config.displayNameAttribute(), "displayName");
			SearchControls controls = new SearchControls();
			controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			controls.setCountLimit(Math.max(1, config.userSearchLimit()));
			controls.setReturningAttributes(new String[] {
					"sAMAccountName",
					"userPrincipalName",
					displayNameAttribute,
					config.fueroAttribute()
			});

			List<UsuarioDominio> usuarios = operations.search(
					config.userSearchBase(),
					config.userSearchFilter(),
					controls,
					(AttributesMapper<UsuarioDominio>) attrs -> toUsuarioDominio(attrs, config))
				.stream()
				.filter(usuario -> !esCuentaAdministrativa(usuario.username()))
				.toList();
			return DominioUsuarios.disponible(usuarios, "");
		} catch (RuntimeException exception) {
			LOGGER.warn("No se pudo consultar Active Directory para listar solicitantes: {}", exception.getMessage());
			return DominioUsuarios.noDisponible("No se pudo consultar Active Directory.", "");
		}
	}

	public DominioUsuarios buscarUsuarios(String query) {
		LdapRuntimeConfig config = currentConfig();
		LdapOperations operations = currentOperations(config);
		String queryNormalizada = query == null ? "" : query.trim();
		if (queryNormalizada.length() < MIN_QUERY_LENGTH) {
			return DominioUsuarios.esperandoBusqueda(
					"Ingrese al menos " + MIN_QUERY_LENGTH + " caracteres para buscar usuarios de dominio.");
		}

		if (!config.enabled()) {
			return DominioUsuarios.noDisponible("LDAP esta desactivado en este entorno.", queryNormalizada);
		}

		if (operations == null) {
			return DominioUsuarios.noDisponible("No hay cliente LDAP de lectura configurado.", queryNormalizada);
		}

		if (StringUtils.hasText(config.readOnlyUserDn())
				&& !StringUtils.hasText(config.readOnlyPassword())) {
			return DominioUsuarios.noDisponible("La cuenta LDAP lectora no tiene clave configurada.", queryNormalizada);
		}

		try {
			String displayNameAttribute = safeAttributeName(config.displayNameAttribute(), "displayName");
			SearchControls controls = new SearchControls();
			controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			controls.setCountLimit(Math.max(1, config.userSearchLimit()));
			controls.setReturningAttributes(new String[] {
					"sAMAccountName",
					"userPrincipalName",
					displayNameAttribute,
					config.fueroAttribute()
			});

			List<UsuarioDominio> usuarios = operations.search(
					config.userSearchBase(),
					buildSearchFilter(queryNormalizada, displayNameAttribute, config),
					controls,
					(AttributesMapper<UsuarioDominio>) attrs -> toUsuarioDominio(attrs, config));
			return DominioUsuarios.disponible(usuarios, queryNormalizada);
		} catch (RuntimeException exception) {
			LOGGER.warn("No se pudo consultar Active Directory para autorizar usuarios: {}", exception.getMessage());
			return DominioUsuarios.noDisponible("No se pudo consultar Active Directory.", queryNormalizada);
		}
	}

	public boolean ldapHabilitado() {
		return currentConfig().enabled();
	}

	public boolean existeUsuario(String username) {
		LdapRuntimeConfig config = currentConfig();
		LdapOperations operations = currentOperations(config);
		if (!StringUtils.hasText(username)) {
			return false;
		}
		if (!config.enabled()) {
			return false;
		}
		if (operations == null) {
			throw new IllegalStateException("No hay cliente LDAP de lectura configurado.");
		}
		if (StringUtils.hasText(config.readOnlyUserDn())
				&& !StringUtils.hasText(config.readOnlyPassword())) {
			throw new IllegalStateException("La cuenta LDAP lectora no tiene clave configurada.");
		}

		try {
			SearchControls controls = new SearchControls();
			controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			controls.setCountLimit(1);
			controls.setReturningAttributes(new String[] { "sAMAccountName" });
			return !operations.search(
					config.userSearchBase(),
					buildExactUserFilter(username, config),
					controls,
					(AttributesMapper<String>) attrs -> firstText(attrs, "sAMAccountName", "")).isEmpty();
		} catch (RuntimeException exception) {
			LOGGER.warn("No se pudo validar el usuario {} contra Active Directory: {}", username, exception.getMessage());
			throw new IllegalStateException("No se pudo validar el usuario contra Active Directory.", exception);
		}
	}

	private static final java.util.Set<String> OUS_IGNORADAS = java.util.Set.of(
			"EQUIPOS", "USUARIOS", "PODJUDSP", "COMPUTERS", "DOMAIN CONTROLLERS", "SYSTEM", "BUILTIN"
	);

	/**
	 * Obtiene todas las Unidades Organizativas (OUs) de Active Directory que representan fueros, juzgados y áreas.
	 */
	public List<String> listarFuerosDesdeAd() {
		LdapRuntimeConfig config = currentConfig();
		LdapOperations operations = currentOperations(config);
		if (!config.enabled() || operations == null) {
			return List.of();
		}
		try {
			SearchControls controls = new SearchControls();
			controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			controls.setReturningAttributes(new String[] { "ou", "name" });
			List<String> ous = operations.search(
					"",
					"(objectClass=organizationalUnit)",
					controls,
					(AttributesMapper<String>) attrs -> {
						Attribute ouAttr = attrs.get("ou");
						if (ouAttr == null || ouAttr.get() == null) {
							ouAttr = attrs.get("name");
						}
						return ouAttr != null && ouAttr.get() != null ? String.valueOf(ouAttr.get()).trim() : null;
					});
			return ous.stream()
					.filter(StringUtils::hasText)
					.filter(ou -> !OUS_IGNORADAS.contains(ou.toUpperCase()))
					.distinct()
					.sorted(String.CASE_INSENSITIVE_ORDER)
					.toList();
		} catch (RuntimeException exception) {
			LOGGER.warn("No se pudieron consultar las Unidades Organizativas (OUs) en Active Directory: {}", exception.getMessage());
			return List.of();
		}
	}

	/**
	 * Busca una computadora en AD por su nombre y deduce su Fuero jerárquico a partir de las OUs de su distinguishedName.
	 */
	public String obtenerFueroDeEquipo(String nombreEquipo) {
		LdapRuntimeConfig config = currentConfig();
		LdapOperations operations = currentOperations(config);
		if (!config.enabled() || operations == null || !StringUtils.hasText(nombreEquipo)) {
			return null;
		}
		try {
			String cleanName = encodeLdapFilterValue(nombreEquipo.trim());
			SearchControls controls = new SearchControls();
			controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			controls.setCountLimit(1);
			controls.setReturningAttributes(new String[] { "distinguishedName" });
			String filter = "(|(sAMAccountName=" + cleanName + "$)(sAMAccountName=" + cleanName + "))";
			List<String> results = operations.search(
					"",
					filter,
					controls,
					(AttributesMapper<String>) attrs -> firstText(attrs, "distinguishedName", null));
			if (results.isEmpty() || !StringUtils.hasText(results.get(0))) {
				return null;
			}
			return parsearFueroDesdeDn(results.get(0));
		} catch (RuntimeException exception) {
			LOGGER.warn("No se pudo consultar el equipo {} en Active Directory: {}", nombreEquipo, exception.getMessage());
			return null;
		}
	}

	/**
	 * Extrae las OUs de un distinguishedName en orden jerárquico general -> específico, ignorando contenedores genéricos.
	 */
	public String parsearFueroDesdeDn(String dn) {
		if (!StringUtils.hasText(dn)) {
			return null;
		}
		String[] parts = dn.split(",");
		java.util.List<String> ous = new java.util.ArrayList<>();
		for (String part : parts) {
			String p = part.trim();
			if (p.toUpperCase().startsWith("OU=")) {
				String ouName = p.substring(3).trim();
				if (!OUS_IGNORADAS.contains(ouName.toUpperCase())) {
					ous.add(ouName);
				}
			}
		}
		if (ous.isEmpty()) {
			return null;
		}
		java.util.Collections.reverse(ous);
		return String.join(" - ", ous);
	}

	private String buildSearchFilter(String query, String displayNameAttribute, LdapRuntimeConfig config) {
		String encodedQuery = encodeLdapFilterValue(query);
		return "(&"
				+ config.userSearchFilter()
				+ "(|"
				+ "(sAMAccountName=*" + encodedQuery + "*)"
				+ "(userPrincipalName=*" + encodedQuery + "*)"
				+ "(" + displayNameAttribute + "=*" + encodedQuery + "*)"
				+ "))";
	}

	private String buildExactUserFilter(String username, LdapRuntimeConfig config) {
		Set<String> candidates = normalizedUserCandidates(username, config);
		StringBuilder filter = new StringBuilder("(&")
				.append(config.userSearchFilter())
				.append("(|");
		for (String candidate : candidates) {
			String encoded = encodeLdapFilterValue(candidate);
			filter.append("(sAMAccountName=").append(encoded).append(")")
					.append("(userPrincipalName=").append(encoded).append(")");
		}
		return filter.append("))").toString();
	}

	private Set<String> normalizedUserCandidates(String username, LdapRuntimeConfig config) {
		String clean = username.trim();
		Set<String> candidates = new LinkedHashSet<>();
		candidates.add(clean);
		int slash = clean.indexOf('\\');
		if (slash >= 0 && slash + 1 < clean.length()) {
			candidates.add(clean.substring(slash + 1));
		}
		int at = clean.indexOf('@');
		if (at > 0) {
			candidates.add(clean.substring(0, at));
		} else if (StringUtils.hasText(config.domain()) && !clean.contains("\\")) {
			candidates.add(clean + "@" + config.domain());
		}
		return candidates;
	}

	private String encodeLdapFilterValue(String value) {
		return value
				.replace("\\", "\\5c")
				.replace("*", "\\2a")
				.replace("(", "\\28")
				.replace(")", "\\29")
				.replace("\u0000", "\\00");
	}

	private String safeAttributeName(String attributeName, String fallback) {
		if (StringUtils.hasText(attributeName) && attributeName.matches(ATTRIBUTE_NAME_PATTERN)) {
			return attributeName;
		}
		return fallback;
	}

	private UsuarioDominio toUsuarioDominio(Attributes attributes, LdapRuntimeConfig config) throws NamingException {
		String username = firstText(attributes, "sAMAccountName", "");
		if (!StringUtils.hasText(username)) {
			username = firstText(attributes, "userPrincipalName", "");
		}
		String nombreVisible = firstText(attributes, config.displayNameAttribute(), username);
		String fuero = firstText(attributes, config.fueroAttribute(), "Sin fuero informado");
		return new UsuarioDominio(username, nombreVisible, fuero);
	}

	private LdapRuntimeConfig currentConfig() {
		return ldapConfigurationService != null ? ldapConfigurationService.current()
				: new LdapRuntimeConfig(
						properties.isEnabled(),
						properties.getUrl(),
						properties.getDomain(),
						properties.getBaseDn(),
						properties.getDisplayNameAttribute(),
						properties.getFueroAttribute(),
						properties.getReadOnlyUserDn(),
						properties.getReadOnlyPassword(),
						properties.getUserSearchBase(),
						properties.getUserSearchFilter(),
						properties.getUserSearchLimit(),
						false);
	}

	private LdapOperations currentOperations(LdapRuntimeConfig config) {
		if (runtimeLdapClientFactory != null && config.enabled()) {
			return runtimeLdapClientFactory.createOperations(config);
		}
		return ldapOperations;
	}

	private String firstText(Attributes attributes, String attributeName, String fallback) throws NamingException {
		if (!StringUtils.hasText(attributeName)) {
			return fallback;
		}
		Attribute attribute = attributes.get(attributeName);
		if (attribute == null || attribute.get() == null) {
			return fallback;
		}
		String value = String.valueOf(attribute.get()).trim();
		return StringUtils.hasText(value) ? value : fallback;
	}

	private boolean esCuentaAdministrativa(String username) {
		return StringUtils.hasText(username) && username.toUpperCase().contains("_ADM");
	}

	public record DominioUsuarios(
			boolean disponible,
			boolean consultaRealizada,
			String query,
			String mensaje,
			List<UsuarioDominio> usuarios) {

		static DominioUsuarios disponible(List<UsuarioDominio> usuarios, String query) {
			return new DominioUsuarios(true, true, query, "Active Directory disponible.", usuarios);
		}

		static DominioUsuarios noDisponible(String mensaje, String query) {
			return new DominioUsuarios(false, true, query, mensaje, List.of());
		}

		static DominioUsuarios esperandoBusqueda(String mensaje) {
			return new DominioUsuarios(false, false, "", mensaje, List.of());
		}
	}

	public record UsuarioDominio(String username, String nombreVisible, String fuero) {
	}
}
