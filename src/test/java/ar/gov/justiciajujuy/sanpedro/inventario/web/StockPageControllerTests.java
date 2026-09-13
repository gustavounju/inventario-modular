package ar.gov.justiciajujuy.sanpedro.inventario.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql({"/sql/limpiar-seguridad-modular-test.sql", "/sql/seguridad-modular-test.sql"})
class StockPageControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void actualizaDatosAdministrativosEnLote() throws Exception {
		mockMvc.perform(get("/admin/stock").with(user(adminLocal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Aplicar a seleccionados")))
				.andExpect(content().string(containsString("Editar")))
				.andExpect(content().string(containsString("stock-type-chip")))
				.andExpect(content().string(not(containsString("Modificar Piezas Cargadas"))))
				.andExpect(content().string(not(containsString("tab-btn-edicion"))));

		mockMvc.perform(post("/admin/stock/componentes/lote")
				.with(user(adminLocal()))
				.with(csrf())
				.param("componentesIds", "1")
				.param("tipo", "MONITOR")
				.param("descripcion", "Monitor LED 22 pulgadas")
				.param("marca", "Samsung")
				.param("modelo", "S22")
				.param("capacidad", "22 pulgadas")
				.param("remito", "R-LOTE-30")
				.param("proveedor", "Proveedor Taller"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/stock?loteActualizado=1#tab-disponibles"));

		mockMvc.perform(get("/admin/stock").with(user(adminLocal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("MONITOR")))
				.andExpect(content().string(containsString("Monitor LED 22 pulgadas")))
				.andExpect(content().string(containsString("Samsung")))
				.andExpect(content().string(containsString("R-LOTE-30")))
				.andExpect(content().string(containsString("Proveedor Taller")));
	}

	@Test
	void creaPendientesDesdeEscaneoRapidoYLuegoLosCompletaEnLote() throws Exception {
		mockMvc.perform(post("/api/v1/stock/componentes/lote-rapido")
				.with(user(adminLocal()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"codigos":["TINTA-001","TINTA-002","TINTA-001","  "]}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].tipo").value("PENDIENTE"))
				.andExpect(jsonPath("$[0].datosCompletos").value(false))
				.andExpect(jsonPath("$[0].ingresadoPor").value("admin.local"));

		mockMvc.perform(get("/admin/stock").with(user(adminLocal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Pendientes de completar")))
				.andExpect(content().string(containsString("tab-btn-pendientes")))
				.andExpect(content().string(containsString("pane-pendientes")))
				.andExpect(content().string(containsString("Completar seleccionados")))
				.andExpect(content().string(not(containsString("Revisar tabla"))))
				.andExpect(content().string(containsString("Falta para Stock")))
				.andExpect(content().string(containsString("Tipo de componente")))
				.andExpect(content().string(containsString("Descripcion real")));

		Long tintaUnoId = jdbcTemplate.queryForObject(
				"SELECT id FROM stock_componentes WHERE serial = 'TINTA-001'", Long.class);
		Long tintaDosId = jdbcTemplate.queryForObject(
				"SELECT id FROM stock_componentes WHERE serial = 'TINTA-002'", Long.class);

		mockMvc.perform(post("/admin/stock/componentes/lote")
				.with(user(adminLocal()))
				.with(csrf())
				.param("componentesIds", String.valueOf(tintaUnoId))
				.param("tipo", "CARTUCHO"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/stock?loteActualizado=1#tab-disponibles"));

		mockMvc.perform(get("/admin/stock").with(user(adminLocal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("CARTUCHO")))
				.andExpect(content().string(containsString("Descripcion real")));

		mockMvc.perform(post("/admin/stock/componentes/lote")
				.with(user(adminLocal()))
				.with(csrf())
				.param("componentesIds", String.valueOf(tintaUnoId), String.valueOf(tintaDosId))
				.param("tipo", "CARTUCHO")
				.param("descripcion", "Cartucho de tinta Epson 664 negro")
				.param("marca", "Epson")
				.param("modelo", "664 BK")
				.param("capacidad", "Negro")
				.param("remito", "R-TINTA-60")
				.param("ordenCompra", "OC-TINTA-2026")
				.param("proveedor", "Proveedor Tintas"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/stock?loteActualizado=2#tab-disponibles"));

		mockMvc.perform(get("/api/v1/stock/componentes").with(user(adminLocal())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.serial == 'TINTA-001')].tipo").value("CARTUCHO"))
				.andExpect(jsonPath("$[?(@.serial == 'TINTA-001')].datosCompletos").value(true))
				.andExpect(jsonPath("$[?(@.serial == 'TINTA-002')].descripcion").value("Cartucho de tinta Epson 664 negro"));

		mockMvc.perform(get("/admin/stock").with(user(adminLocal())))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString("tab-btn-pendientes"))))
				.andExpect(content().string(containsString("Cartucho de tinta Epson 664 negro")));
	}

	@Test
	void sincronizaComoAsignadoSiTieneVinculoActivoConEquipo() throws Exception {
		jdbcTemplate.update("""
				INSERT INTO stock_componentes
					(id, tipo, estado, descripcion, marca, modelo, serial, capacidad, activo)
				VALUES
					(20, 'DISCO', 'DISPONIBLE', 'Disco ya instalado mal marcado', 'Kingston', 'SA400', 'DISK-001', '480GB', TRUE)
				""");

		mockMvc.perform(get("/admin/stock").with(user(adminLocal())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Disco ya instalado mal marcado")))
				.andExpect(content().string(containsString("PC-INF-001")))
				.andExpect(content().string(containsString("ASIGNADO")));

		String estado = jdbcTemplate.queryForObject(
				"SELECT estado FROM stock_componentes WHERE id = 20", String.class);
		org.assertj.core.api.Assertions.assertThat(estado).isEqualTo("ASIGNADO");
	}

	@Test
	void loteRequiereSeleccionYDatos() throws Exception {
		mockMvc.perform(post("/admin/stock/componentes/lote")
				.with(user(adminLocal()))
				.with(csrf())
				.param("remito", "R-SIN-SELECCION"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/stock?errorLote=seleccion#tab-disponibles"));

		mockMvc.perform(post("/admin/stock/componentes/lote")
				.with(user(adminLocal()))
				.with(csrf())
				.param("componentesIds", "1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/stock?errorLote=datos#tab-disponibles"));
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
}
