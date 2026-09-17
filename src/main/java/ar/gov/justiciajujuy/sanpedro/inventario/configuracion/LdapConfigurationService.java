package ar.gov.justiciajujuy.sanpedro.inventario.configuracion;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ar.gov.justiciajujuy.sanpedro.inventario.config.ActiveDirectoryProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LdapConfigurationService {

	private static final String PREFIX = "ldap.";
	private static final String ENABLED = PREFIX + "enabled";
	private static final String URL = PREFIX + "url";
	private static final String DOMAIN = PREFIX + "domain";
	private static final String BASE_DN = PREFIX + "baseDn";
	private static final String DISPLAY_NAME_ATTRIBUTE = PREFIX + "displayNameAttribute";
	private static final String FUERO_ATTRIBUTE = PREFIX + "fueroAttribute";
	private static final String READ_ONLY_USER_DN = PREFIX + "readOnlyUserDn";
	private static final String READ_ONLY_PASSWORD = PREFIX + "readOnlyPassword";
	private static final String USER_SEARCH_BASE = PREFIX + "userSearchBase";
	private static final String USER_SEARCH_FILTER = PREFIX + "userSearchFilter";
	private static final String USER_SEARCH_LIMIT = PREFIX + "userSearchLimit";

	private final SistemaConfiguracionRepository repository;
	private final ActiveDirectoryProperties properties;
	private final SecretProtector secretProtector;

	public LdapConfigurationService(
			SistemaConfiguracionRepository repository,
			ActiveDirectoryProperties properties,
			SecretProtector secretProtector) {
		this.repository = repository;
		this.properties = properties;
		this.secretProtector = secretProtector;
	}

	@Transactional(readOnly = true)
	public LdapRuntimeConfig current() {
		Map<String, String> values = persistedValues();
		boolean persisted = values.containsKey(ENABLED);
		if (!persisted) {
			return fromProperties(false);
		}
		return new LdapRuntimeConfig(
				Boolean.parseBoolean(values.getOrDefault(ENABLED, "false")),
				values.getOrDefault(URL, properties.getUrl()),
				values.getOrDefault(DOMAIN, properties.getDomain()),
				values.getOrDefault(BASE_DN, properties.getBaseDn()),
				values.getOrDefault(DISPLAY_NAME_ATTRIBUTE, properties.getDisplayNameAttribute()),
				values.getOrDefault(FUERO_ATTRIBUTE, properties.getFueroAttribute()),
				values.getOrDefault(READ_ONLY_USER_DN, properties.getReadOnlyUserDn()),
				reveal(values.get(READ_ONLY_PASSWORD)),
				values.getOrDefault(USER_SEARCH_BASE, properties.getUserSearchBase()),
				values.getOrDefault(USER_SEARCH_FILTER, properties.getUserSearchFilter()),
				parseLimit(values.get(USER_SEARCH_LIMIT), properties.getUserSearchLimit()),
				true);
	}

	@Transactional
	public void save(SaveLdapConfig command) {
		requireText(command.url(), "URL LDAP");
		requireText(command.domain(), "Dominio");
		requireText(command.baseDn(), "Base DN de login");
		requireText(command.userSearchBase(), "Base de busqueda de usuarios");
		requireText(command.readOnlyUserDn(), "Usuario lector");
		requireText(command.readOnlyPassword(), "Clave del usuario lector");
		put(ENABLED, String.valueOf(command.enabled()));
		put(URL, command.url().trim());
		put(DOMAIN, command.domain().trim());
		put(BASE_DN, command.baseDn().trim());
		put(DISPLAY_NAME_ATTRIBUTE, textOrDefault(command.displayNameAttribute(), "displayName"));
		put(FUERO_ATTRIBUTE, textOrDefault(command.fueroAttribute(), "department"));
		put(READ_ONLY_USER_DN, command.readOnlyUserDn().trim());
		put(READ_ONLY_PASSWORD, secretProtector.protect(command.readOnlyPassword()));
		put(USER_SEARCH_BASE, command.userSearchBase().trim());
		put(USER_SEARCH_FILTER, textOrDefault(command.userSearchFilter(), "(&(objectClass=user)(!(objectClass=computer)))"));
		put(USER_SEARCH_LIMIT, String.valueOf(Math.max(1, command.userSearchLimit())));
	}

	@Transactional(readOnly = true)
	public LdapRuntimeConfig preview(SaveLdapConfig command) {
		return new LdapRuntimeConfig(
				command.enabled(),
				textOrDefault(command.url(), properties.getUrl()),
				textOrDefault(command.domain(), properties.getDomain()),
				textOrDefault(command.baseDn(), properties.getBaseDn()),
				textOrDefault(command.displayNameAttribute(), "displayName"),
				textOrDefault(command.fueroAttribute(), "department"),
				textOrDefault(command.readOnlyUserDn(), properties.getReadOnlyUserDn()),
				textOrDefault(command.readOnlyPassword(), current().readOnlyPassword()),
				textOrDefault(command.userSearchBase(), properties.getUserSearchBase()),
				textOrDefault(command.userSearchFilter(), properties.getUserSearchFilter()),
				Math.max(1, command.userSearchLimit()),
				true);
	}

	public LdapRuntimeConfig fromProperties(boolean persisted) {
		return new LdapRuntimeConfig(
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
				persisted);
	}

	public boolean persistentAdEnabled() {
		try {
			return current().persisted() && current().enabled();
		} catch (RuntimeException exception) {
			return false;
		}
	}

	private Map<String, String> persistedValues() {
		try {
			return repository.findAllById(java.util.List.of(
							ENABLED, URL, DOMAIN, BASE_DN, DISPLAY_NAME_ATTRIBUTE, FUERO_ATTRIBUTE,
							READ_ONLY_USER_DN, READ_ONLY_PASSWORD, USER_SEARCH_BASE, USER_SEARCH_FILTER,
							USER_SEARCH_LIMIT))
					.stream()
					.collect(Collectors.toMap(SistemaConfiguracion::getClave, SistemaConfiguracion::getValor));
		} catch (DataAccessException exception) {
			return Map.of();
		}
	}

	private String reveal(String value) {
		return StringUtils.hasText(value) ? secretProtector.reveal(value) : "";
	}

	private void put(String key, String value) {
		Optional<SistemaConfiguracion> existente = repository.findById(key);
		SistemaConfiguracion configuracion = existente.orElseGet(() -> new SistemaConfiguracion(key, value));
		configuracion.setValor(value);
		repository.save(configuracion);
	}

	private int parseLimit(String value, int fallback) {
		try {
			return StringUtils.hasText(value) ? Integer.parseInt(value) : fallback;
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	private void requireText(String value, String label) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(label + " es obligatorio.");
		}
	}

	private String textOrDefault(String value, String fallback) {
		return StringUtils.hasText(value) ? value.trim() : fallback;
	}

	public record SaveLdapConfig(
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
	}
}
