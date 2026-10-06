package ar.gov.justiciajujuy.sanpedro.inventario.configuracion;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ar.gov.justiciajujuy.sanpedro.inventario.config.TelefoniaProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TelefoniaConfigurationService {

	private static final String PREFIX = "telefonia.";
	private static final String ENABLED = PREFIX + "enabled";
	private static final String URL = PREFIX + "url";
	private static final String USER = PREFIX + "user";
	private static final String PASSWORD = PREFIX + "password";
	private static final String INTERNO = PREFIX + "interno";
	private static final String WEBHOOK_IPS = PREFIX + "webhookIps";
	private static final String SSL_STRICT = PREFIX + "sslStrict";

	private final SistemaConfiguracionRepository repository;
	private final TelefoniaProperties properties;
	private final SecretProtector secretProtector;

	public TelefoniaConfigurationService(
			SistemaConfiguracionRepository repository,
			TelefoniaProperties properties,
			SecretProtector secretProtector) {
		this.repository = repository;
		this.properties = properties;
		this.secretProtector = secretProtector;
	}

	@Transactional(readOnly = true)
	public TelefoniaRuntimeConfig current() {
		Map<String, String> values = persistedValues();
		boolean persisted = values.containsKey(ENABLED);
		if (!persisted) {
			return fromProperties(false);
		}
		return new TelefoniaRuntimeConfig(
				Boolean.parseBoolean(values.getOrDefault(ENABLED, "false")),
				values.getOrDefault(URL, properties.getUrl()),
				values.getOrDefault(USER, properties.getUser()),
				reveal(values.get(PASSWORD)),
				values.getOrDefault(INTERNO, properties.getInterno()),
				values.getOrDefault(WEBHOOK_IPS, properties.getWebhookIps()),
				Boolean.parseBoolean(values.getOrDefault(SSL_STRICT, String.valueOf(properties.isSslStrict()))),
				true);
	}

	@Transactional
	public void save(SaveTelefoniaConfig command) {
		requireText(command.url(), "URL de la central telefónica");
		requireText(command.user(), "Usuario API");
		requireText(command.interno(), "Interno monitoreado");

		put(ENABLED, String.valueOf(command.enabled()));
		put(URL, command.url().trim());
		put(USER, command.user().trim());
		if (StringUtils.hasText(command.password())) {
			put(PASSWORD, secretProtector.protect(command.password().trim()));
		}
		put(INTERNO, command.interno().trim());
		put(WEBHOOK_IPS, textOrDefault(command.webhookIps(), properties.getWebhookIps()));
		put(SSL_STRICT, String.valueOf(command.sslStrict()));
	}

	@Transactional(readOnly = true)
	public TelefoniaRuntimeConfig preview(SaveTelefoniaConfig command) {
		return new TelefoniaRuntimeConfig(
				command.enabled(),
				textOrDefault(command.url(), properties.getUrl()),
				textOrDefault(command.user(), properties.getUser()),
				textOrDefault(command.password(), current().password()),
				textOrDefault(command.interno(), properties.getInterno()),
				textOrDefault(command.webhookIps(), properties.getWebhookIps()),
				command.sslStrict(),
				true);
	}

	public TelefoniaRuntimeConfig fromProperties(boolean persisted) {
		return new TelefoniaRuntimeConfig(
				properties.isEnabled(),
				properties.getUrl(),
				properties.getUser(),
				properties.getPassword(),
				properties.getInterno(),
				properties.getWebhookIps(),
				properties.isSslStrict(),
				persisted);
	}

	private Map<String, String> persistedValues() {
		try {
			return repository.findAllById(java.util.List.of(
							ENABLED, URL, USER, PASSWORD, INTERNO, WEBHOOK_IPS, SSL_STRICT))
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

	private void requireText(String value, String label) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(label + " es obligatorio.");
		}
	}

	private String textOrDefault(String value, String fallback) {
		return StringUtils.hasText(value) ? value.trim() : fallback;
	}

	public record SaveTelefoniaConfig(
			boolean enabled,
			String url,
			String user,
			String password,
			String interno,
			String webhookIps,
			boolean sslStrict) {
	}
}
