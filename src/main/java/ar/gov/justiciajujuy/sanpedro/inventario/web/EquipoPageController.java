package ar.gov.justiciajujuy.sanpedro.inventario.web;

import ar.gov.justiciajujuy.sanpedro.inventario.equipos.EquipoService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class EquipoPageController {

	private static final String MODULO_EQUIPOS = "EQUIPOS";
	private static final String PERMISO_VER = "VER";

	private final AuthorizationService authorizationService;
	private final EquipoService equipoService;

	public EquipoPageController(AuthorizationService authorizationService, EquipoService equipoService) {
		this.authorizationService = authorizationService;
		this.equipoService = equipoService;
	}

	@GetMapping("/admin/equipos")
	public String equipos(
			Model model,
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam(required = false) String q) {
		if (!authorizationService.tienePermiso(userDetails, MODULO_EQUIPOS, PERMISO_VER)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para ver equipos.");
		}
		model.addAttribute("query", q == null ? "" : q.trim());
		model.addAttribute("equipos", equipoService.listar(q, 0, 50));
		return "admin/equipos";
	}
}
