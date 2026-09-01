package ar.gov.justiciajujuy.sanpedro.inventario.web;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class CurrentUserControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void exposesCurrentUserWithAllowedModules() throws Exception {
		ActiveDirectoryUserDetails userDetails = new ActiveDirectoryUserDetails(
				"admin.local",
				"",
				List.of(new SimpleGrantedAuthority("ROLE_USER")),
				"Nombre desde AD",
				"Fuero desde AD",
				Map.of());

		mockMvc.perform(get("/api/v1/me").with(user(userDetails)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username", is("admin.local")))
			.andExpect(jsonPath("$.nombreVisible", is("Administrador Local")))
			.andExpect(jsonPath("$.fuero", is("Desarrollo local")))
			.andExpect(jsonPath("$.autorizado", is(true)))
			.andExpect(jsonPath("$.modulos", hasSize(6)))
			.andExpect(jsonPath("$.modulos[0].codigo", is("EQUIPOS")))
			.andExpect(jsonPath("$.modulos[0].permisos", containsInAnyOrder("ADMINISTRAR", "EDITAR", "VER")))
			.andExpect(jsonPath("$.modulos[1].codigo", is("STOCK")))
			.andExpect(jsonPath("$.modulos[2].codigo", is("ORDENES_ARMADO")))
			.andExpect(jsonPath("$.modulos[3].codigo", is("COMPONENTES")))
			.andExpect(jsonPath("$.modulos[5].codigo", is("AUDITORIA")));
	}

	@Test
	void returnsEmptyModulesForAuthenticatedUserWithoutLocalAuthorization() throws Exception {
		ActiveDirectoryUserDetails userDetails = new ActiveDirectoryUserDetails(
				"usuario.ad",
				"",
				List.of(new SimpleGrantedAuthority("ROLE_USER")),
				"Usuario AD",
				"Informatica",
				Map.of());

		mockMvc.perform(get("/api/v1/me/modulos").with(user(userDetails)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
	}
}
