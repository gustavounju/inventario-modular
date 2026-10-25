package ar.gov.justiciajujuy.sanpedro.inventario.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.gov.justiciajujuy.sanpedro.inventario.config.ActiveDirectoryProperties;
import ar.gov.justiciajujuy.sanpedro.inventario.config.LocalAuthenticationProperties;
import ar.gov.justiciajujuy.sanpedro.inventario.config.NetworkAccessProperties;
import org.junit.jupiter.api.Test;

class SecurityStartupValidatorTests {

	private static final String VALID_REMEMBER_ME_KEY = "clave-recordarme-de-prueba-con-mas-de-32-caracteres";

	private SecurityStartupValidator validator(String reportToken) {
		return validator(reportToken, VALID_REMEMBER_ME_KEY);
	}

	private SecurityStartupValidator validator(String reportToken, String rememberMeKey) {
		ActiveDirectoryProperties ad = new ActiveDirectoryProperties();
		ad.setEnabled(false);
		NetworkAccessProperties network = new NetworkAccessProperties();
		network.setLanOnly(false);
		return new SecurityStartupValidator(ad, new LocalAuthenticationProperties(), network,
				true, "", reportToken, rememberMeKey);
	}

	@Test
	void rechazaClaveRecordarmeVacia() {
		assertThatThrownBy(() -> validator("", "").run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("remember-me-key");
	}

	@Test
	void rechazaClaveRecordarmeCorta() {
		assertThatThrownBy(() -> validator("", "menos-de-32-caracteres").run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("remember-me-key");
	}

	@Test
	void rechazaTokenPorDefectoHistorico() {
		assertThatThrownBy(() -> validator("inventario-lan-token-2026").run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("report-token");
	}

	@Test
	void incluyeElHashDelTokenPublicadoEnElRepositorio() {
		// El token hexadecimal publicado no se repite en el codigo fuente: se verifica por su SHA-256.
		org.assertj.core.api.Assertions.assertThat(SecurityStartupValidator.LEAKED_REPORT_TOKEN_SHA256)
				.contains("8391a161f34c4489eef7300cc14f6f2f89f80744540208602d01836028e76f04");
	}

	@Test
	void permiteTokenVacioComoDeshabilitado() {
		assertThatCode(() -> validator("").run(null)).doesNotThrowAnyException();
	}

	@Test
	void permiteTokenNuevo() {
		assertThatCode(() -> validator("token-nuevo-generado-aleatoriamente-0123456789").run(null))
				.doesNotThrowAnyException();
	}
}
