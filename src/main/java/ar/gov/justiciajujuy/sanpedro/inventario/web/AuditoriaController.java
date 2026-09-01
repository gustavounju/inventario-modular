package ar.gov.justiciajujuy.sanpedro.inventario.web;

import java.util.List;

import ar.gov.justiciajujuy.sanpedro.inventario.auditoria.AuditoriaService;
import ar.gov.justiciajujuy.sanpedro.inventario.auditoria.AuditoriaService.AuditoriaEventoDetalle;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {

	private static final String MODULO_AUDITORIA = "AUDITORIA";
	private static final String PERMISO_VER = "VER";

	private final AuthorizationService authorizationService;
	private final AuditoriaService auditoriaService;

	public AuditoriaController(AuthorizationService authorizationService, AuditoriaService auditoriaService) {
		this.authorizationService = authorizationService;
		this.auditoriaService = auditoriaService;
	}

	@GetMapping("/eventos")
	public List<AuditoriaEventoDetalle> listar(@AuthenticationPrincipal UserDetails userDetails) {
		if (!authorizationService.tienePermiso(userDetails, MODULO_AUDITORIA, PERMISO_VER)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para ver auditoria.");
		}
		return auditoriaService.listarRecientes();
	}
}
