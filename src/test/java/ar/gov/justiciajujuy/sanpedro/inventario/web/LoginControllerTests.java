package ar.gov.justiciajujuy.sanpedro.inventario.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class LoginControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void muestraComandoDeInventarioParaCopiarDesdeLogin() throws Exception {
		mockMvc.perform(get("/login"))
			.andExpect(status().isOk())
			.andExpect(view().name("login"))
			.andExpect(content().string(containsString("Copiar comando")))
			.andExpect(content().string(containsString("/scripts/windows/inventario-modular.ps1")))
			.andExpect(content().string(containsString("/api/v1/equipos/inventario")));
	}

	@Test
	void permiteDescargarScriptDeInventarioSinLogin() throws Exception {
		mockMvc.perform(get("/scripts/windows/inventario-modular.ps1"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("param(")))
			.andExpect(content().string(containsString("discosModelos")))
			.andExpect(content().string(containsString("motherboardSerial")));
	}
}
