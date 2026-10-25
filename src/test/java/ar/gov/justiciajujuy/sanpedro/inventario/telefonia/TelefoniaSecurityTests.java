package ar.gov.justiciajujuy.sanpedro.inventario.telefonia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(properties = {
		"ucm.webhook.ips=10.15.0.2,127.0.0.1",
		"ucm.simulacion.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/limpiar-seguridad-modular-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/seguridad-modular-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TelefoniaSecurityTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TelefoniaEventService eventService;

	@Test
	void simularSinLoginDevuelve401o403() throws Exception {
		mockMvc.perform(post("/api/telefonia/simular")
				.with(csrf())
				.param("caller", "2005"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void simularConLoginSinPermisoDevuelve403() throws Exception {
		mockMvc.perform(post("/api/telefonia/simular")
				.with(user(usuarioSinPermisos()))
				.with(csrf())
				.param("caller", "2005"))
				.andExpect(status().isForbidden());
	}

	@Test
	void simularConAdminPeroSimulacionDeshabilitadaDevuelve403() throws Exception {
		mockMvc.perform(post("/api/telefonia/simular")
				.with(user(adminLocal()))
				.with(csrf())
				.param("caller", "2005"))
				.andExpect(status().isForbidden());
	}

	@Test
	void webhookDesdeIpNoPermitidaConXForwardedForFalsificadoDevuelve403() throws Exception {
		// Peticion directa desde 192.168.1.99 (no loopback): X-Forwarded-For debe ser ignorado.
		mockMvc.perform(post("/api/telefonia/webhook/llamada")
				.with(request -> {
					request.setRemoteAddr("192.168.1.99");
					return request;
				})
				.header("X-Forwarded-For", "10.15.0.2")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"caller\":\"2005\",\"event\":\"RINGING\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void webhookDesdeLoopbackConXForwardedForPermitidoDevuelve200() throws Exception {
		// Conexion local de proxy reverso (127.0.0.1) que reenvia la IP autorizada de la UCM.
		mockMvc.perform(post("/api/telefonia/webhook/llamada")
				.with(request -> {
					request.setRemoteAddr("127.0.0.1");
					return request;
				})
				.header("X-Forwarded-For", "10.15.0.2")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"caller\":\"2005\",\"event\":\"RINGING\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void endpointsVisorSiguenPublicosSinLogin() throws Exception {
		mockMvc.perform(get("/api/telefonia/historial-hoy"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/telefonia/directorio"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/telefonia/stream"))
				.andExpect(status().isOk());
	}

	@Test
	void limitaConexionesSseSimultaneas() {
		TelefoniaEventService localService = new TelefoniaEventService();
		for (int i = 0; i < TelefoniaEventService.MAX_CONEXIONES_SIMULTANEAS; i++) {
			localService.crearConexion();
		}
		assertThat(localService.cantidadConexionesActivas()).isEqualTo(TelefoniaEventService.MAX_CONEXIONES_SIMULTANEAS);

		assertThatThrownBy(localService::crearConexion)
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
						.isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
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
