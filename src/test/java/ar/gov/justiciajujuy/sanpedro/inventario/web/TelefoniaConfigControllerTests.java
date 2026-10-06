package ar.gov.justiciajujuy.sanpedro.inventario.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Map;

import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/limpiar-seguridad-modular-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/seguridad-modular-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TelefoniaConfigControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void permiteAccesoAPantallaCentralTelefonicaSoloAAdministradores() throws Exception {
		mockMvc.perform(get("/admin/sistema/telefonia").with(user(adminLocal())))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/sistema-telefonia"))
				.andExpect(content().string(containsString("Central Telefónica")))
				.andExpect(content().string(containsString("URL Servidor UCM")))
				.andExpect(content().string(containsString("Interno del Taller / Guardia")))
				.andExpect(content().string(containsString("Probar conexión")))
				.andExpect(content().string(containsString("Guardar configuración")));
	}

	@Test
	void rechazaAccesoAUsuariosSinPermisoDeAdministracion() throws Exception {
		mockMvc.perform(get("/admin/sistema/telefonia").with(user(usuarioSinPermisos())))
				.andExpect(status().isForbidden());
	}

	@Test
	void guardaConfiguracionDesdeFormularioWeb() throws Exception {
		mockMvc.perform(post("/admin/sistema/telefonia")
				.with(user(adminLocal()))
				.with(csrf())
				.param("enabled", "true")
				.param("url", "https://10.15.0.2:8089/api")
				.param("user", "cdrapi")
				.param("password", "nuevaClave123")
				.param("interno", "1005")
				.param("webhookIps", "10.15.0.2, 127.0.0.1")
				.param("sslStrict", "false"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/sistema-telefonia"))
				.andExpect(content().string(containsString("Configuración")));

		// Al volver a consultar, debe mostrar la URL guardada
		mockMvc.perform(get("/admin/sistema/telefonia").with(user(adminLocal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("10.15.0.2:8089/api")))
				.andExpect(content().string(containsString("1005")));
	}

	@Test
	void ejecutaPruebaDeConexionDesdeFormularioWeb() throws Exception {
		mockMvc.perform(post("/admin/sistema/telefonia/probar")
				.with(user(adminLocal()))
				.with(csrf())
				.param("enabled", "true")
				.param("url", "https://127.0.0.1:18089/api")
				.param("user", "testuser")
				.param("password", "testpass")
				.param("interno", "1005")
				.param("webhookIps", "127.0.0.1")
				.param("sslStrict", "false"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/sistema-telefonia"));
	}

	private ActiveDirectoryUserDetails adminLocal() {
		return new ActiveDirectoryUserDetails(
				"admin.local",
				"unused",
				List.of(new SimpleGrantedAuthority("ROLE_USER")),
				"Administrador Local",
				"Desarrollo local",
				Map.of("origen", List.of("LOCAL_SIMULADO")));
	}

	private ActiveDirectoryUserDetails usuarioSinPermisos() {
		return new ActiveDirectoryUserDetails(
				"sin.permisos",
				"unused",
				List.of(new SimpleGrantedAuthority("ROLE_USER")),
				"Usuario Sin Permisos",
				"Mesa de ayuda",
				Map.of("origen", List.of("AD_TEST")));
	}
}
