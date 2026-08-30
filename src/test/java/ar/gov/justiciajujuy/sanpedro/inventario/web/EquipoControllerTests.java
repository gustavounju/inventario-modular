package ar.gov.justiciajujuy.sanpedro.inventario.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/limpiar-seguridad-modular-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/seguridad-modular-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EquipoControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void listaEquiposConPaginacionSiTienePermisoVer() throws Exception {
		mockMvc.perform(get("/api/v1/equipos").with(user(adminLocal())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.equipos", hasSize(2)))
			.andExpect(jsonPath("$.equipos[0].nombre").value("PC-INF-001"))
			.andExpect(jsonPath("$.paginacion.totalItems").value(2));
	}

	@Test
	void filtraEquiposPorNombreUsuarioOFuero() throws Exception {
		mockMvc.perform(get("/api/v1/equipos")
				.param("q", "mesa")
				.with(user(adminLocal())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.equipos", hasSize(1)))
			.andExpect(jsonPath("$.equipos[0].nombre").value("PC-MESA-002"));
	}

	@Test
	void devuelveDetalleDeEquipo() throws Exception {
		mockMvc.perform(get("/api/v1/equipos/1").with(user(adminLocal())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombre").value("PC-INF-001"))
			.andExpect(jsonPath("$.procesador").value("Intel Core i5"))
			.andExpect(jsonPath("$.ramMb").value(16384));
	}

	@Test
	void registraInventarioComoAltaOActualizacionDeEquipo() throws Exception {
		String body = """
				{
				  "nombre": "pc-nueva-003",
				  "ultimoUsuario": "jlopez",
				  "fuero": "Informatica",
				  "ip": "10.15.2.12",
				  "sistemaOperativo": "Windows 11 Pro",
				  "procesador": "AMD Ryzen 5",
				  "ramMb": 16384,
				  "impresora": "Ricoh Mesa",
				  "activo": true
				}
				""";

		mockMvc.perform(post("/api/v1/equipos/inventario")
				.with(user(adminLocal()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.nombre").value("PC-NUEVA-003"))
			.andExpect(jsonPath("$.ultimoUsuario").value("jlopez"))
			.andExpect(jsonPath("$.monitoreo").value("REPORTADO"));
	}

	@Test
	void bloqueaUsuariosSinPermisoParaVerEquipos() throws Exception {
		mockMvc.perform(get("/api/v1/equipos").with(user(usuarioSinPermisos())))
			.andExpect(status().isForbidden());
	}

	@Test
	void bloqueaUsuariosSinPermisoParaRegistrarInventario() throws Exception {
		String body = """
				{
				  "nombre": "pc-bloqueada",
				  "fuero": "Informatica",
				  "activo": true
				}
				""";

		mockMvc.perform(post("/api/v1/equipos/inventario")
				.with(user(usuarioSinPermisos()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
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
				"sin.permisos",
				"unused",
				List.of(new SimpleGrantedAuthority("ROLE_USER")),
				"Usuario Sin Permisos",
				"Mesa de ayuda",
				Map.of("origen", List.of("AD_TEST")));
	}
}
