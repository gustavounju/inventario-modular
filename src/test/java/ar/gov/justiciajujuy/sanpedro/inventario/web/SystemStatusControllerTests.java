package ar.gov.justiciajujuy.sanpedro.inventario.web;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemStatusControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void exposesPublicSystemStatus() throws Exception {
		mockMvc.perform(get("/api/v1/sistema/estado"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("application/json"))
			.andExpect(jsonPath("$.estado", is("OPERATIVO")))
			.andExpect(jsonPath("$.aplicacion", is("Inventario Modular")))
			.andExpect(jsonPath("$.version", is("0.0.1-SNAPSHOT")))
			.andExpect(jsonPath("$.perfil", is("test")));
	}

	@Test
	void redirectsRootToAdminPanel() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin"));
	}

	@Test
	void rendersPublicAdminEntryPoint() throws Exception {
		mockMvc.perform(get("/admin"))
			.andExpect(status().isOk())
			.andExpect(view().name("admin/index"));
	}
}
