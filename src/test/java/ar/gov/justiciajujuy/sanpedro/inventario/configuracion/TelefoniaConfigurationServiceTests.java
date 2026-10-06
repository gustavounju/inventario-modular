package ar.gov.justiciajujuy.sanpedro.inventario.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import ar.gov.justiciajujuy.sanpedro.inventario.config.TelefoniaProperties;
import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.TelefoniaConfigurationService.SaveTelefoniaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/sql/limpiar-seguridad-modular-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/seguridad-modular-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TelefoniaConfigurationServiceTests {

	@Autowired
	private SistemaConfiguracionRepository repository;

	@Autowired
	private SecretProtector secretProtector;

	private TelefoniaProperties properties;
	private TelefoniaConfigurationService service;

	@BeforeEach
	void setUp() {
		properties = new TelefoniaProperties();
		properties.setEnabled(true);
		properties.setUrl("https://10.15.0.2:8089/api");
		properties.setUser("cdrapi");
		properties.setPassword("defaultSecret");
		properties.setInterno("1005");
		properties.setWebhookIps("10.15.0.2,127.0.0.1");
		properties.setSslStrict(false);

		service = new TelefoniaConfigurationService(repository, properties, secretProtector);
	}

	@Test
	void devuelveValoresPorDefectoSiNoHayConfiguracionPersistida() {
		TelefoniaRuntimeConfig current = service.current();
		assertThat(current.persisted()).isFalse();
		assertThat(current.enabled()).isTrue();
		assertThat(current.url()).isEqualTo("https://10.15.0.2:8089/api");
		assertThat(current.user()).isEqualTo("cdrapi");
		assertThat(current.interno()).isEqualTo("1005");
		assertThat(current.sslStrict()).isFalse();
		assertThat(current.isIpAllowed("10.15.0.2")).isTrue();
		assertThat(current.isIpAllowed("192.168.1.100")).isFalse();
	}

	@Test
	void guardaYRecuperaConfiguracionCifradaEnBaseDeDatos() {
		SaveTelefoniaConfig command = new SaveTelefoniaConfig(
				true,
				"https://10.15.0.99:8089/api",
				"admin_ucm",
				"claveSecreta123",
				"2001",
				"10.15.0.99, 127.0.0.1",
				true);

		service.save(command);

		TelefoniaRuntimeConfig current = service.current();
		assertThat(current.persisted()).isTrue();
		assertThat(current.enabled()).isTrue();
		assertThat(current.url()).isEqualTo("https://10.15.0.99:8089/api");
		assertThat(current.user()).isEqualTo("admin_ucm");
		assertThat(current.password()).isEqualTo("claveSecreta123");
		assertThat(current.interno()).isEqualTo("2001");
		assertThat(current.webhookIps()).isEqualTo("10.15.0.99, 127.0.0.1");
		assertThat(current.sslStrict()).isTrue();

		// Verificar que en la base de datos la clave esté cifrada y no en texto plano
		Optional<SistemaConfiguracion> passEntity = repository.findById("telefonia.password");
		assertThat(passEntity).isPresent();
		assertThat(passEntity.get().getValor()).startsWith("v1:");
		assertThat(passEntity.get().getValor()).doesNotContain("claveSecreta123");
	}

	@Test
	void previewGeneraConfiguracionSinPersistir() {
		SaveTelefoniaConfig command = new SaveTelefoniaConfig(
				false,
				"https://10.15.0.50:8089/api",
				"previewUser",
				"previewPass",
				"1002",
				"10.15.0.50",
				false);

		TelefoniaRuntimeConfig preview = service.preview(command);
		assertThat(preview.enabled()).isFalse();
		assertThat(preview.url()).isEqualTo("https://10.15.0.50:8089/api");
		assertThat(preview.user()).isEqualTo("previewUser");
		assertThat(preview.interno()).isEqualTo("1002");

		// El servicio actual sigue devolviendo valores de propiedades
		assertThat(service.current().persisted()).isFalse();
	}
}
