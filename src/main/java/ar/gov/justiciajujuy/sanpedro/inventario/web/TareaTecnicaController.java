package ar.gov.justiciajujuy.sanpedro.inventario.web;

import java.util.List;

import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.EstadoTareaTecnica;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.PrioridadTareaTecnica;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.AgregarComentarioTareaCommand;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.CambiarEstadoTareaCommand;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.EquipoNoEncontradoException;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.GuardarTareaTecnicaCommand;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.InstalarStockReservadoCommand;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.RegistrarUsoStockCommand;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.SolicitanteDominioNoEncontradoException;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.StockComponenteNoDisponibleParaTareaException;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.StockComponenteNoEncontradoException;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.StockComponenteNoReservadoException;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.StockDisponibleDetalle;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.StockUsoNoEncontradoException;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.TareaComentarioDetalle;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.TareaEquipoGenericoException;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.TareaStockUsoNoEncontradoException;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.TareaStockUsoDetalle;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.TareaTecnicaDetalle;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.TareaTecnicaNoEncontradaException;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.TareaTecnicaYaAsignadaException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/tareas-tecnicas")
public class TareaTecnicaController {

	private static final String MODULO_TAREAS = "TAREAS";
	private static final String PERMISO_VER = "VER";
	private static final String PERMISO_CREAR = "CREAR";
	private static final String PERMISO_EDITAR = "EDITAR";

	private final AuthorizationService authorizationService;
	private final TareaTecnicaService tareaTecnicaService;

	public TareaTecnicaController(AuthorizationService authorizationService, TareaTecnicaService tareaTecnicaService) {
		this.authorizationService = authorizationService;
		this.tareaTecnicaService = tareaTecnicaService;
	}

	@GetMapping
	public List<TareaTecnicaDetalle> listar(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam(required = false) EstadoTareaTecnica estado,
			@RequestParam(required = false) Long equipoId,
			@RequestParam(required = false) String responsable) {
		exigirPermiso(userDetails, PERMISO_VER);
		List<TareaTecnicaDetalle> tareas = tareaTecnicaService.buscar(estado, equipoId, responsable);
		if (esTelefonistaSinGestionTecnica(userDetails)) {
			return tareas.stream()
					.filter(this::estaAbierta)
					.toList();
		}
		return tareas;
	}

	@GetMapping("/stock-disponible")
	public List<StockDisponibleDetalle> stockDisponible(@AuthenticationPrincipal UserDetails userDetails) {
		exigirPermiso(userDetails, PERMISO_VER);
		return tareaTecnicaService.stockDisponibleParaTareas();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TareaTecnicaDetalle crear(
			@AuthenticationPrincipal UserDetails userDetails,
			@Valid @RequestBody GuardarTareaTecnicaRequest request) {
		exigirPermisoCrear(userDetails);
		return tareaTecnicaService.crear(request.toCommand(userDetails, true, false));
	}

	@PutMapping("/{id}")
	public TareaTecnicaDetalle actualizar(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@Valid @RequestBody GuardarTareaTecnicaRequest request) {
		exigirPuedeActualizar(userDetails, id);
		return tareaTecnicaService.actualizar(id, request.toCommand(userDetails, true, false));
	}

	@PostMapping("/{id}/tomar")
	public TareaTecnicaDetalle tomar(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id) {
		exigirPermiso(userDetails, PERMISO_EDITAR);
		return tareaTecnicaService.tomar(id, userDetails.getUsername());
	}

	@PostMapping("/{id}/soltar")
	public TareaTecnicaDetalle soltar(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id) {
		exigirPermiso(userDetails, PERMISO_EDITAR);
		exigirTareaPropiaOAdministrador(userDetails, id);
		return tareaTecnicaService.soltar(id, userDetails.getUsername());
	}

	@PatchMapping("/{id}/estado")
	public TareaTecnicaDetalle cambiarEstado(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@Valid @RequestBody CambiarEstadoTareaRequest request) {
		exigirPermiso(userDetails, PERMISO_EDITAR);
		if (!authorizationService.puedeAdministrarUsuarios(userDetails)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Solo los administradores pueden cambiar el estado de las tareas.");
		}
		return tareaTecnicaService.cambiarEstado(id, request.toCommand(), userDetails.getUsername());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id) {
		exigirPuedeEliminar(userDetails, id);
		tareaTecnicaService.eliminar(id);
	}

	@GetMapping("/{id}/comentarios")
	public List<TareaComentarioDetalle> comentarios(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id) {
		exigirPermiso(userDetails, PERMISO_VER);
		return tareaTecnicaService.comentarios(id);
	}

	@PostMapping("/{id}/comentarios")
	@ResponseStatus(HttpStatus.CREATED)
	public TareaComentarioDetalle comentar(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@Valid @RequestBody AgregarComentarioTareaRequest request) {
		exigirPuedeComentar(userDetails, id);
		return tareaTecnicaService.comentar(id, request.toCommand(userDetails.getUsername()));
	}

	@GetMapping("/{id}/stock")
	public List<TareaStockUsoDetalle> stockUsado(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id) {
		exigirPermiso(userDetails, PERMISO_VER);
		return tareaTecnicaService.stockUsado(id);
	}

	@PostMapping("/{id}/stock")
	@ResponseStatus(HttpStatus.CREATED)
	public TareaStockUsoDetalle registrarUsoStock(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@Valid @RequestBody RegistrarUsoStockRequest request) {
		exigirPermiso(userDetails, PERMISO_EDITAR);
		exigirTareaPropiaOAdministrador(userDetails, id);
		return tareaTecnicaService.registrarUsoStock(id, request.toCommand(userDetails.getUsername()));
	}

	@PostMapping("/{id}/stock/{usoStockId}/instalar")
	public TareaTecnicaDetalle instalarStockReservado(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@PathVariable Long usoStockId,
			@Valid @RequestBody InstalarStockReservadoRequest request) {
		exigirPermiso(userDetails, PERMISO_EDITAR);
		exigirTareaPropiaOAdministrador(userDetails, id);
		return tareaTecnicaService.instalarStockReservadoEnEquipo(id, usoStockId,
				request.toCommand(userDetails.getUsername()));
	}

	private void exigirPermiso(UserDetails userDetails, String permiso) {
		if (!authorizationService.tienePermiso(userDetails, MODULO_TAREAS, permiso)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para operar tareas tecnicas.");
		}
	}

	private void exigirPermisoCrear(UserDetails userDetails) {
		if (!authorizationService.tienePermiso(userDetails, MODULO_TAREAS, PERMISO_CREAR)
				&& !authorizationService.tienePermiso(userDetails, MODULO_TAREAS, PERMISO_EDITAR)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para crear tareas tecnicas.");
		}
	}

	private void exigirPuedeActualizar(UserDetails userDetails, Long tareaId) {
		TareaTecnicaDetalle tarea = tareaTecnicaService.obtener(tareaId);
		if (authorizationService.puedeAdministrarUsuarios(userDetails)
				|| esCreador(tarea, userDetails)) {
			return;
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN,
				"Solo el creador de la tarea o un administrador pueden editarla.");
	}

	private void exigirPuedeComentar(UserDetails userDetails, Long tareaId) {
		TareaTecnicaDetalle tarea = tareaTecnicaService.obtener(tareaId);
		if (authorizationService.puedeAdministrarUsuarios(userDetails)
				|| esResponsable(tarea, userDetails)
				|| esCreador(tarea, userDetails)) {
			return;
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN,
				"Solo puede comentar tareas propias o asignadas.");
	}

	private void exigirPuedeEliminar(UserDetails userDetails, Long tareaId) {
		if (authorizationService.puedeAdministrarUsuarios(userDetails)) {
			return;
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN,
				"Solo los administradores pueden eliminar tareas técnicas.");
	}

	private void exigirTareaPropiaOAdministrador(UserDetails userDetails, Long tareaId) {
		if (authorizationService.puedeAdministrarUsuarios(userDetails)) {
			return;
		}
		TareaTecnicaDetalle tarea = tareaTecnicaService.obtener(tareaId);
		if (!esResponsable(tarea, userDetails) && !esCreador(tarea, userDetails)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"La tarea debe estar tomada por el tecnico en sesion o haber sido creada por el.");
		}
	}

	private boolean puedeGestionarComoTecnico(UserDetails userDetails, TareaTecnicaDetalle tarea) {
		return authorizationService.tienePermiso(userDetails, MODULO_TAREAS, PERMISO_EDITAR)
				&& (esResponsable(tarea, userDetails) || esCreador(tarea, userDetails));
	}

	private boolean puedeOperarTareaPropiaNoTomada(UserDetails userDetails, TareaTecnicaDetalle tarea) {
		// Telefonista puede corregir o borrar su alta solo mientras la tarea sigue libre y abierta.
		return authorizationService.tienePermiso(userDetails, MODULO_TAREAS, PERMISO_CREAR)
				&& esCreador(tarea, userDetails)
				&& !org.springframework.util.StringUtils.hasText(tarea.responsable())
				&& estaAbierta(tarea);
	}

	private boolean esTelefonistaSinGestionTecnica(UserDetails userDetails) {
		// TAREAS/CREAR sin TAREAS/EDITAR identifica mesa telefonica: crea y comenta, no toma trabajos.
		return authorizationService.tienePermiso(userDetails, MODULO_TAREAS, PERMISO_CREAR)
				&& !authorizationService.tienePermiso(userDetails, MODULO_TAREAS, PERMISO_EDITAR)
				&& !authorizationService.puedeAdministrarUsuarios(userDetails);
	}

	private boolean esResponsable(TareaTecnicaDetalle tarea, UserDetails userDetails) {
		return tarea.responsable() != null && tarea.responsable().equalsIgnoreCase(userDetails.getUsername());
	}

	private boolean esCreador(TareaTecnicaDetalle tarea, UserDetails userDetails) {
		return tarea.creadoPor() != null && tarea.creadoPor().equalsIgnoreCase(userDetails.getUsername());
	}

	private boolean estaAbierta(TareaTecnicaDetalle tarea) {
		return tarea.estado() == EstadoTareaTecnica.PENDIENTE || tarea.estado() == EstadoTareaTecnica.EN_PROCESO;
	}

	@ExceptionHandler({TareaTecnicaNoEncontradaException.class, EquipoNoEncontradoException.class,
			StockComponenteNoEncontradoException.class, StockUsoNoEncontradoException.class,
			TareaStockUsoNoEncontradoException.class})
	@ResponseStatus(HttpStatus.NOT_FOUND)
	void noEncontrado() {
	}

	@ExceptionHandler({TareaTecnicaYaAsignadaException.class, StockComponenteNoDisponibleParaTareaException.class,
			StockComponenteNoReservadoException.class})
	@ResponseStatus(HttpStatus.CONFLICT)
	void yaAsignada() {
	}

	@ExceptionHandler(TareaEquipoGenericoException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	void equipoGenerico() {
	}

	@ExceptionHandler(SolicitanteDominioNoEncontradoException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	void solicitanteNoExisteEnAd() {
	}

	public record GuardarTareaTecnicaRequest(
			Long equipoId,
			@NotBlank @Size(max = 180) String titulo,
			@Size(max = 1000) String descripcion,
			@NotBlank @Size(max = 120) String solicitanteUsername,
			@NotBlank @Size(max = 180) String solicitanteNombre,
			@NotBlank @Size(max = 120) String solicitanteFuero,
			PrioridadTareaTecnica prioridad,
			@Size(max = 120) String responsable) {

		private GuardarTareaTecnicaCommand toCommand(UserDetails userDetails, boolean puedeAsignarResponsable,
				boolean autoAsignarCreador) {
			String responsableFinal = puedeAsignarResponsable ? responsable : (autoAsignarCreador ? userDetails.getUsername() : null);
			return new GuardarTareaTecnicaCommand(
					equipoId,
					titulo,
					descripcion,
					solicitanteUsername,
					solicitanteNombre,
					solicitanteFuero,
					prioridad,
					responsableFinal,
					userDetails.getUsername());
		}
	}

	public record CambiarEstadoTareaRequest(
			@NotNull EstadoTareaTecnica estado,
			@Size(max = 1000) String observacionesCierre) {

		private CambiarEstadoTareaCommand toCommand() {
			return new CambiarEstadoTareaCommand(estado, observacionesCierre);
		}
	}

	public record AgregarComentarioTareaRequest(
			@NotBlank @Size(max = 1000) String comentario) {

		private AgregarComentarioTareaCommand toCommand(String autor) {
			return new AgregarComentarioTareaCommand(autor, comentario);
		}
	}

	public record RegistrarUsoStockRequest(
			@NotNull Long stockComponenteId,
			@Size(max = 500) String observacion) {

		private RegistrarUsoStockCommand toCommand(String registradoPor) {
			return new RegistrarUsoStockCommand(stockComponenteId, registradoPor, observacion);
		}
	}

	public record InstalarStockReservadoRequest(
			@NotNull Long equipoId,
			@Size(max = 120) String ubicacion) {

		private InstalarStockReservadoCommand toCommand(String registradoPor) {
			return new InstalarStockReservadoCommand(equipoId, ubicacion, registradoPor);
		}
	}
}
