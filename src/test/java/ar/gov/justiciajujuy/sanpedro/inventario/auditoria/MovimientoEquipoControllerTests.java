package ar.gov.justiciajujuy.sanpedro.inventario.auditoria;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class MovimientoEquipoControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void verHistorialSinPermisoDevuelve403() throws Exception {
		mockMvc.perform(get("/admin/equipos/1/auditoria").with(user(usuarioSinPermisos())))
				.andExpect(status().isForbidden());
	}

	@Test
	void verHistorialConTokenMaquinaDevuelve403() throws Exception {
		mockMvc.perform(get("/admin/equipos/1/auditoria").with(user(usuarioMaquina())))
				.andExpect(status().isForbidden());
	}

	@Test
	void verHistorialConPermisoDevuelveOk() throws Exception {
		mockMvc.perform(get("/admin/equipos/1/auditoria").with(user(adminLocal())))
				.andExpect(status().isOk());
	}

	@Test
	void registrarMovimientoSinPermisoDevuelve403() throws Exception {
		mockMvc.perform(post("/admin/equipos/1/auditoria")
				.with(user(usuarioSinPermisos()))
				.with(csrf())
				.param("tipoMovimiento", "ENTREGA"))
				.andExpect(status().isForbidden());
	}

	@Test
	void registrarMovimientoConTokenMaquinaDevuelve403() throws Exception {
		mockMvc.perform(post("/admin/equipos/1/auditoria")
				.with(user(usuarioMaquina()))
				.with(csrf())
				.param("tipoMovimiento", "ENTREGA"))
				.andExpect(status().isForbidden());
	}

	@Test
	void registrarMovimientoConPermisoRegistraYRedirige() throws Exception {
		mockMvc.perform(post("/admin/equipos/1/auditoria")
				.with(user(adminLocal()))
				.with(csrf())
				.param("tipoMovimiento", "ENTREGA")
				.param("observaciones", "Entrega formal"))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	void descargarPdfSinPermisoDevuelve403() throws Exception {
		mockMvc.perform(get("/admin/equipos/1/auditoria/1/pdf").with(user(usuarioSinPermisos())))
				.andExpect(status().isForbidden());
	}

	@Test
	void descargarPdfConTokenMaquinaDevuelve403() throws Exception {
		mockMvc.perform(get("/admin/equipos/1/auditoria/1/pdf").with(user(usuarioMaquina())))
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

	private ActiveDirectoryUserDetails usuarioMaquina() {
		return new ActiveDirectoryUserDetails(
				"maquina",
				"unused",
				List.of(new SimpleGrantedAuthority("ROLE_MACHINE")),
				"Maquina LAN",
				"Infraestructura",
				Map.of("origen", List.of("TOKEN_MAQUINA")));
	}
}
