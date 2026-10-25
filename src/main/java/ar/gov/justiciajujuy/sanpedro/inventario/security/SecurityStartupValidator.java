package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ar.gov.justiciajujuy.sanpedro.inventario.config.ActiveDirectoryProperties;
import ar.gov.justiciajujuy.sanpedro.inventario.config.LocalAuthenticationProperties;
import ar.gov.justiciajujuy.sanpedro.inventario.config.NetworkAccessProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("!test")
public class SecurityStartupValidator implements ApplicationRunner {

	private static final Pattern JDBC_MYSQL = Pattern.compile("jdbc:mysql://([^:/?]+)(?::\\d+)?/.*");

	static final int MIN_REMEMBER_ME_KEY_LENGTH = 32;

	/**
	 * SHA-256 de tokens de reporte que fueron publicados (repositorio, script .ps1 o valores por defecto historicos).
	 * Se guardan como hash para no volver a publicar el valor en el codigo fuente.
	 */
	static final Set<String> LEAKED_REPORT_TOKEN_SHA256 = Set.of(
			"8391a161f34c4489eef7300cc14f6f2f89f80744540208602d01836028e76f04",
			"705a0c82686b98d860f7bd2334cee5784f5fe76b603a3429839fa939f3119a44");

	private final ActiveDirectoryProperties activeDirectoryProperties;
	private final LocalAuthenticationProperties localAuthenticationProperties;
	private final NetworkAccessProperties networkAccessProperties;
	private final boolean localDbAuthenticationEnabled;
	private final String dataSourceUrl;
	private final String reportToken;
	private final String rememberMeKey;

	public SecurityStartupValidator(
			ActiveDirectoryProperties activeDirectoryProperties,
			LocalAuthenticationProperties localAuthenticationProperties,
			NetworkAccessProperties networkAccessProperties,
			@Value("${inventario.local-db-auth.enabled:false}") boolean localDbAuthenticationEnabled,
			@Value("${spring.datasource.url:}") String dataSourceUrl,
			@Value("${inventario.security.report-token:}") String reportToken,
			@Value("${inventario.security.remember-me-key:}") String rememberMeKey) {
		this.activeDirectoryProperties = activeDirectoryProperties;
		this.localAuthenticationProperties = localAuthenticationProperties;
		this.networkAccessProperties = networkAccessProperties;
		this.localDbAuthenticationEnabled = localDbAuthenticationEnabled;
		this.dataSourceUrl = dataSourceUrl;
		this.reportToken = reportToken;
		this.rememberMeKey = rememberMeKey;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!StringUtils.hasText(rememberMeKey) || rememberMeKey.trim().length() < MIN_REMEMBER_ME_KEY_LENGTH) {
			throw new IllegalStateException(
					"inventario.security.remember-me-key debe tener al menos " + MIN_REMEMBER_ME_KEY_LENGTH
							+ " caracteres. Defina una clave aleatoria en la variable INVENTARIO_REMEMBER_ME_KEY.");
		}
		// Token vacio = token de maquina deshabilitado (permitido).
		if (StringUtils.hasText(reportToken) && isLeakedReportToken(reportToken)) {
			throw new IllegalStateException(
					"inventario.security.report-token utiliza un valor que fue publicado y debe considerarse comprometido. Genere un token nuevo y definalo en la variable INVENTARIO_REPORT_TOKEN.");
		}

		if (!activeDirectoryProperties.isEnabled()
				&& !localAuthenticationProperties.isEnabled()
				&& !localDbAuthenticationEnabled) {
			throw new IllegalStateException(
					"Debe configurar al menos un metodo de autenticacion: Active Directory, usuario local explicito o usuarios locales de base.");
		}

		if (localAuthenticationProperties.isEnabled()) {
			requireText(localAuthenticationProperties.getUsername(), "inventario.local-auth.username");
			requireText(localAuthenticationProperties.getPassword(), "inventario.local-auth.password");
		}

		if (networkAccessProperties.isLanOnly()) {
			NetworkAddressPolicy addressPolicy = new NetworkAddressPolicy(networkAccessProperties.getAllowedCidrs());
			requireLanHost(extractJdbcMysqlHost(dataSourceUrl), "spring.datasource.url", addressPolicy);
			if (activeDirectoryProperties.isEnabled()) {
				requireLanHost(extractUriHost(activeDirectoryProperties.getUrl()), "inventario.ldap.url", addressPolicy);
			}
		}
	}

	static boolean isLeakedReportToken(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.trim().getBytes(StandardCharsets.UTF_8));
			return LEAKED_REPORT_TOKEN_SHA256.contains(HexFormat.of().formatHex(digest));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 no disponible en la JVM", ex);
		}
	}

	private void requireText(String value, String propertyName) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalStateException("Falta configurar " + propertyName + ".");
		}
	}

	private void requireLanHost(String host, String propertyName, NetworkAddressPolicy addressPolicy) {
		if (StringUtils.hasText(host) && !addressPolicy.isAllowed(host)) {
			throw new IllegalStateException(propertyName + " apunta fuera de la LAN permitida: " + host);
		}
	}

	private String extractJdbcMysqlHost(String jdbcUrl) {
		if (!StringUtils.hasText(jdbcUrl)) {
			return "";
		}
		Matcher matcher = JDBC_MYSQL.matcher(jdbcUrl);
		return matcher.matches() ? matcher.group(1) : "";
	}

	private String extractUriHost(String url) {
		if (!StringUtils.hasText(url)) {
			return "";
		}
		return URI.create(url).getHost();
	}
}
