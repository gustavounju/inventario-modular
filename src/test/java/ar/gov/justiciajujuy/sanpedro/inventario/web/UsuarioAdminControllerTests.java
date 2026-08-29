package ar.gov.justiciajujuy.sanpedro.inventario.web;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/limpiar-seguridad-modular-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/seguridad-modular-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UsuarioAdminControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void listaUsuariosAutorizadosCuandoElUsuarioAdministraElModuloUsuarios() throws Exception {
		mockMvc.perform(get("/api/v1/usuarios").with(user(adminLocal())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.usuarios", hasSize(2)))
			.andExpect(jsonPath("$.usuarios[0].username").value("admin.local"))
			.andExpect(jsonPath("$.usuarios[0].roles", hasItem("ADMINISTRADOR")));
	}

	@Test
	void listaRolesDisponiblesParaAdministrarUsuarios() throws Exception {
		mockMvc.perform(get("/api/v1/roles").with(user(adminLocal())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.roles", hasSize(1)))
			.andExpect(jsonPath("$.roles[0].codigo").value("ADMINISTRADOR"));
	}

	@Test
	void creaUsuarioDeDominioConRolesLocalesSinGuardarClave() throws Exception {
		String body = """
				{
				  "username": "gmurad",
				  "nombreVisible": "Gustavo Elias Murad",
				  "fuero": "Informatica",
				  "activo": true,
				  "roles": ["ADMINISTRADOR"]
				}
				""";

		mockMvc.perform(post("/api/v1/usuarios")
				.with(user(adminLocal()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.username").value("gmurad"))
			.andExpect(jsonPath("$.nombreVisible").value("Gustavo Elias Murad"))
			.andExpect(jsonPath("$.roles", hasItem("ADMINISTRADOR")));
	}

	@Test
	void rechazaUsuariosDuplicados() throws Exception {
		String body = """
				{
				  "username": "admin.local",
				  "nombreVisible": "Administrador Local",
				  "fuero": "Desarrollo local",
				  "activo": true,
				  "roles": ["ADMINISTRADOR"]
				}
				""";

		mockMvc.perform(post("/api/v1/usuarios")
				.with(user(adminLocal()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isConflict());
	}

	@Test
	void rechazaRolesInexistentesAlCrearUsuario() throws Exception {
		String body = """
				{
				  "username": "tecnico.local",
				  "nombreVisible": "Tecnico Local",
				  "fuero": "Informatica",
				  "activo": true,
				  "roles": ["ROL_QUE_NO_EXISTE"]
				}
				""";

		mockMvc.perform(post("/api/v1/usuarios")
				.with(user(adminLocal()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	void bloqueaUsuariosSinPermisoParaAdministrarUsuarios() throws Exception {
		mockMvc.perform(get("/api/v1/usuarios").with(user(usuarioSinPermisos())))
			.andExpect(status().isForbidden());
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
				"usuario.ad",
				"unused",
				List.of(new SimpleGrantedAuthority("ROLE_USER")),
				"Usuario AD",
				"Mesa de ayuda",
				Map.of("origen", List.of("AD_TEST")));
	}
}
