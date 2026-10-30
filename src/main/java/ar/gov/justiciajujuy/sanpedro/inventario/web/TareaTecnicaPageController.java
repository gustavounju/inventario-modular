package ar.gov.justiciajujuy.sanpedro.inventario.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestBody;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaConciliacionService;

import ar.gov.justiciajujuy.sanpedro.inventario.equipos.EquipoRepository;
import ar.gov.justiciajujuy.sanpedro.inventario.equipos.FueroService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryDomainService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryDomainService.DominioUsuarios;
import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryDomainService.UsuarioDominio;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.UsuarioManagementService;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.EstadoTareaTecnica;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.PrioridadTareaTecnica;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TipoTareaTecnica;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.AgregarComentarioTareaCommand;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.CambiarEstadoTareaCommand;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.GuardarTareaTecnicaCommand;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.InstalarStockReservadoCommand;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.RegistrarUsoStockCommand;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.TareaComentarioDetalle;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.TareaStockUsoDetalle;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.TareaTecnicaDetalle;
import ar.gov.justiciajujuy.sanpedro.inventario.movil.ApkPresenciaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TareaTecnicaPageController {

	private static final String MODULO_TAREAS = "TAREAS";
	private static final String PERMISO_VER = "VER";
	private static final String PERMISO_EDITAR = "EDITAR";

	private final AuthorizationService authorizationService;
	private final TareaTecnicaService tareaTecnicaService;
	private final EquipoRepository equipoRepository;
	private final ActiveDirectoryDomainService activeDirectoryDomainService;
	private final UsuarioManagementService usuarioManagementService;
	private final FueroService fueroService;
	private final ApkPresenciaService apkPresenciaService;
	private final TareaConciliacionService tareaConciliacionService;

	public TareaTecnicaPageController(
			AuthorizationService authorizationService,
			TareaTecnicaService tareaTecnicaService,
			EquipoRepository equipoRepository,
			ActiveDirectoryDomainService activeDirectoryDomainService,
			UsuarioManagementService usuarioManagementService,
			FueroService fueroService,
			ApkPresenciaService apkPresenciaService,
			TareaConciliacionService tareaConciliacionService) {
		this.authorizationService = authorizationService;
		this.tareaTecnicaService = tareaTecnicaService;
		this.equipoRepository = equipoRepository;
		this.activeDirectoryDomainService = activeDirectoryDomainService;
		this.usuarioManagementService = usuarioManagementService;
		this.fueroService = fueroService;
		this.apkPresenciaService = apkPresenciaService;
		this.tareaConciliacionService = tareaConciliacionService;
	}

	@GetMapping("/admin/tareas")
	public String tareas(
			Model model,
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(required = false, defaultValue = "false") boolean todas,
			@RequestParam(required = false) EstadoTareaTecnica estado,
			@RequestParam(required = false) Long equipoId,
			@RequestParam(required = false) String responsable,
			@RequestParam(required = false) String creado) {
		exigirPermiso(userDetails, PERMISO_VER);
		if (authorizationService.esSoloMovil(userDetails)) {
			return "redirect:/movil/tareas";
		}
		prepararModelo(model, userDetails, new TareaForm(), estado, equipoId, responsable, false, fecha, todas);
		model.addAttribute("creado", "1".equals(creado));
		return "admin/tareas";
	}

	@GetMapping("/admin/tareas/visor")
	public String visorTareas(
			Model model,
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(required = false, defaultValue = "false") boolean todas,
			@RequestParam(required = false) EstadoTareaTecnica estado,
			@RequestParam(required = false) Long equipoId,
			@RequestParam(required = false) String responsable,
			@RequestParam(required = false) String creado) {
		prepararModelo(model, userDetails, new TareaForm(), estado, equipoId, responsable, true, fecha, todas);
		model.addAttribute("creado", "1".equals(creado));
		return "admin/tareas-visor";
	}

	@GetMapping("/api/v1/tareas/solicitantes")
	@ResponseBody
	public DominioUsuarios buscarSolicitantes(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam(name = "q", required = false) String query) {
		exigirPermiso(userDetails, PERMISO_VER);
		return activeDirectoryDomainService.buscarSolicitantesParaTareas(query);
	}

	@GetMapping("/api/v1/tareas/conciliacion/analisis")
	@ResponseBody
	public TareaConciliacionService.ResumenConciliacion analizarConciliacion(
			@AuthenticationPrincipal UserDetails userDetails) {
		exigirPermiso(userDetails, PERMISO_EDITAR);
		return tareaConciliacionService.analizar();
	}

	@PostMapping("/api/v1/tareas/conciliacion/aplicar")
	@ResponseBody
	public TareaConciliacionService.ResultadoConciliacion aplicarConciliacion(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestBody(required = false) List<Long> tareaIds) {
		exigirPermiso(userDetails, PERMISO_EDITAR);
		return tareaConciliacionService.aplicar(tareaIds);
	}


	@PostMapping("/admin/tareas")
	public String crear(
			Model model,
			@AuthenticationPrincipal UserDetails userDetails,
			@Valid @ModelAttribute("tareaForm") TareaForm tareaForm,
			BindingResult bindingResult,
			@RequestParam(required = false) String origen,
			RedirectAttributes redirectAttributes) {
		try {
			exigirAdministradorTareas(userDetails);
			if (tareaForm.getTipo() == null) {
				tareaForm.setTipo(TipoTareaTecnica.INCIDENTE);
			}
			if (tareaForm.getTipo().requiereSolicitante() && !StringUtils.hasText(tareaForm.getSolicitanteUsername())) {
				redirectAttributes.addFlashAttribute("error", "El solicitante es obligatorio para requerimientos e incidentes.");
				return redireccionTareas(origen);
			}
			tareaForm.aplicarReglaResponsable(userDetails.getUsername(), puedeAdministrarTareas(userDetails));
			if (bindingResult.hasErrors()) {
				redirectAttributes.addFlashAttribute("error", "Por favor complete todos los campos obligatorios para guardar la tarea.");
				return redireccionTareas(origen);
			}
			tareaTecnicaService.crear(tareaForm.toCommand(userDetails != null ? userDetails.getUsername() : null));
			redirectAttributes.addAttribute("creado", "1");
			redirectAttributes.addFlashAttribute("mensajeExito", "Tarea técnica creada exitosamente.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error al crear la tarea: " + e.getMessage());
		}
		return redireccionTareas(origen);
	}

	@PostMapping("/admin/tareas/{id}")
	public String actualizar(
			Model model,
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@Valid @ModelAttribute("tareaForm") TareaForm tareaForm,
			BindingResult bindingResult,
			@RequestParam(required = false) String origen,
			RedirectAttributes redirectAttributes) {
		try {
			exigirAdministradorTareas(userDetails);
			exigirTareaPropiaOAdministrador(userDetails, id);
			tareaForm.aplicarReglaResponsable(userDetails.getUsername(), puedeAdministrarTareas(userDetails));
			if (bindingResult.hasErrors()) {
				redirectAttributes.addFlashAttribute("error", "Por favor revise los campos de la tarea.");
				return redireccionTareas(origen);
			}
			tareaTecnicaService.actualizar(id, tareaForm.toCommand(userDetails != null ? userDetails.getUsername() : null));
			redirectAttributes.addAttribute("creado", "1");
			redirectAttributes.addFlashAttribute("mensajeExito", "Tarea técnica actualizada.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error al actualizar la tarea: " + e.getMessage());
		}
		return redireccionTareas(origen);
	}

	@PostMapping("/admin/tareas/{id}/tomar")
	public String tomar(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@RequestParam(required = false) String responsable,
			@RequestParam(required = false) String origen,
			RedirectAttributes redirectAttributes) {
		try {
			exigirAdministradorTareas(userDetails);
			String tecnico = org.springframework.util.StringUtils.hasText(responsable) ? responsable : (userDetails != null ? userDetails.getUsername() : "");
			tareaTecnicaService.tomar(id, tecnico);
			redirectAttributes.addAttribute("creado", "1");
			redirectAttributes.addFlashAttribute("mensajeExito", "Tarea asignada a " + tecnico);
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error al tomar/asignar la tarea: " + e.getMessage());
		}
		return redireccionTareas(origen);
	}

	@PostMapping("/admin/tareas/{id}/estado")
	public String cambiarEstado(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@RequestParam EstadoTareaTecnica estado,
			@RequestParam(required = false) String responsable,
			@RequestParam(required = false) String observacionesCierre,
			@RequestParam(required = false) String origen,
			RedirectAttributes redirectAttributes) {
		try {
			exigirAdministradorTareas(userDetails);
			exigirTareaPropiaOAdministrador(userDetails, id);
			if (org.springframework.util.StringUtils.hasText(responsable)) {
				try {
					tareaTecnicaService.tomar(id, responsable.trim());
				} catch (Exception ignored) {
				}
			}
			tareaTecnicaService.cambiarEstado(id, new CambiarEstadoTareaCommand(estado, observacionesCierre), userDetails != null ? userDetails.getUsername() : null);
			redirectAttributes.addAttribute("creado", "1");
			redirectAttributes.addFlashAttribute("mensajeExito", "Estado actualizado a " + estado);
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "No se pudo cambiar el estado: " + e.getMessage());
		}
		return redireccionTareas(origen);
	}

	@PostMapping("/admin/tareas/{id}/soltar")
	public String soltar(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@RequestParam(required = false) String origen,
			RedirectAttributes redirectAttributes) {
		try {
			exigirAdministradorTareas(userDetails);
			exigirTareaPropiaOAdministrador(userDetails, id);
			tareaTecnicaService.soltar(id, userDetails != null ? userDetails.getUsername() : null);
			redirectAttributes.addAttribute("creado", "1");
			redirectAttributes.addFlashAttribute("mensajeExito", "Tarea liberada y devuelta a Nuevas.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error al soltar la tarea: " + e.getMessage());
		}
		return redireccionTareas(origen);
	}

	@PostMapping("/admin/tareas/{id}/eliminar")
	public String eliminar(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@RequestParam(required = false) String origen,
			RedirectAttributes redirectAttributes) {
		try {
			exigirAdministradorTareas(userDetails);
			exigirTareaPropiaOAdministrador(userDetails, id);
			tareaTecnicaService.eliminar(id);
			redirectAttributes.addFlashAttribute("eliminado", true);
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error al eliminar la tarea: " + e.getMessage());
		}
		return redireccionTareas(origen);
	}

	@PostMapping("/admin/tareas/{id}/comentarios")
	public String comentar(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@RequestParam @NotBlank @Size(max = 1000) String comentario,
			@RequestParam(required = false) String origen,
			RedirectAttributes redirectAttributes) {
		exigirAdministradorTareas(userDetails);
		exigirTareaPropiaOAdministrador(userDetails, id);
		tareaTecnicaService.comentar(id, new AgregarComentarioTareaCommand(userDetails.getUsername(), comentario));
		redirectAttributes.addAttribute("creado", "1");
		return redireccionTareas(origen);
	}

	@PostMapping("/admin/tareas/{id}/stock")
	public String registrarUsoStock(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@RequestParam Long stockComponenteId,
			@RequestParam(required = false) String observacion,
			@RequestParam(required = false) String origen,
			RedirectAttributes redirectAttributes) {
		exigirAdministradorTareas(userDetails);
		exigirTareaPropiaOAdministrador(userDetails, id);
		tareaTecnicaService.registrarUsoStock(id,
				new RegistrarUsoStockCommand(stockComponenteId, userDetails.getUsername(), observacion));
		redirectAttributes.addAttribute("creado", "1");
		return redireccionTareas(origen);
	}

	@PostMapping("/admin/tareas/{id}/stock/{usoStockId}/instalar")
	public String instalarStockReservado(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@PathVariable Long usoStockId,
			@RequestParam Long equipoId,
			@RequestParam(required = false) String ubicacion,
			@RequestParam(required = false) String origen,
			RedirectAttributes redirectAttributes) {
		exigirAdministradorTareas(userDetails);
		exigirTareaPropiaOAdministrador(userDetails, id);
		tareaTecnicaService.instalarStockReservadoEnEquipo(id, usoStockId,
				new InstalarStockReservadoCommand(equipoId, ubicacion, userDetails.getUsername()));
		redirectAttributes.addAttribute("creado", "1");
		return redireccionTareas(origen);
	}

	private void prepararModelo(Model model, UserDetails userDetails, TareaForm tareaForm,
			EstadoTareaTecnica estado, Long equipoId, String responsable, boolean esVisor,
			LocalDate fecha, boolean todas) {
		boolean tieneFiltro = estado != null || equipoId != null || StringUtils.hasText(responsable);
		boolean esModoTodas = todas || (tieneFiltro && fecha == null);
		LocalDate hoy = LocalDate.now();
		LocalDate fechaFiltro = (fecha != null) ? fecha : (esModoTodas ? null : hoy);

		List<TareaTecnicaDetalle> tareas;
		if (esModoTodas) {
			tareas = tieneFiltro
					? tareaTecnicaService.buscar(estado, equipoId, responsable)
					: (esVisor ? tareaTecnicaService.buscarParaVisor() : tareaTecnicaService.buscar(null, null, null));
		} else {
			tareas = tareaTecnicaService.buscarPorFecha(fechaFiltro, estado, equipoId, responsable);
		}

		Set<Long> tareaIds = tareas.stream().map(TareaTecnicaDetalle::id).collect(Collectors.toSet());
		Map<Long, List<TareaComentarioDetalle>> comentariosPorTarea = tareaTecnicaService.comentariosPorTareas(tareaIds);
		Map<Long, List<TareaStockUsoDetalle>> stockPorTarea = tareaTecnicaService.stockUsadoPorTareas(tareaIds);

		boolean autenticado = userDetails != null;
		boolean puedeEditarTareas = puedeAdministrarTareas(userDetails);
		boolean puedeAsignarResponsable = puedeEditarTareas;

		model.addAttribute("tareas", tareas);
		model.addAttribute("comentariosPorTarea", comentariosPorTarea);
		model.addAttribute("stockPorTarea", stockPorTarea);
		model.addAttribute("resumenTareas", tareaTecnicaService.resumenDelDia());
		model.addAttribute("tareaForm", tareaForm);
		model.addAttribute("estadosTarea", EstadoTareaTecnica.values());
		model.addAttribute("prioridadesTarea", PrioridadTareaTecnica.values());
		model.addAttribute("tiposTarea", TipoTareaTecnica.values());
		model.addAttribute("filtroEstado", estado);
		model.addAttribute("filtroEquipoId", equipoId);
		model.addAttribute("filtroResponsable", responsable);
		model.addAttribute("filtroFecha", fechaFiltro);
		model.addAttribute("esModoTodas", todas);
		model.addAttribute("fechaHoy", hoy);
		model.addAttribute("puedeEditarTareas", puedeEditarTareas);
		model.addAttribute("puedeAsignarResponsable", puedeAsignarResponsable);
		model.addAttribute("usuarioActual", autenticado ? userDetails.getUsername() : "");
		model.addAttribute("tecnicosConectados", apkPresenciaService.obtenerConectados());
		model.addAttribute("esFiltroActivo", tieneFiltro);
		model.addAttribute("esSoloMovil", authorizationService.esSoloMovil(userDetails));
		model.addAttribute("esAdministrador", authorizationService.esAdministrador(userDetails));

		if (autenticado) {
			model.addAttribute("stockDisponibleParaTareas", tareaTecnicaService.stockDisponibleParaTareas());
			model.addAttribute("equipos", equipoRepository.buscar(null, org.springframework.data.domain.Pageable.unpaged()).getContent().stream()
					.filter(equipo -> !TareaTecnicaService.EQUIPO_GENERICO_NOMBRE.equalsIgnoreCase(equipo.getNombre()))
					.toList());
			model.addAttribute("equipoGenericoId", equipoRepository.findByNombreIgnoreCase(TareaTecnicaService.EQUIPO_GENERICO_NOMBRE)
					.map(ar.gov.justiciajujuy.sanpedro.inventario.equipos.Equipo::getId)
					.orElse(null));
			List<UsuarioDominio> solicitantes = solicitantesParaTareas();
			var tecnicos = usuarioManagementService.listarTecnicosAsignables();
			model.addAttribute("solicitantes", solicitantes);
			model.addAttribute("fuerosDisponibles", fuerosParaTareas(solicitantes));
			model.addAttribute("tecnicosAsignables", tecnicos);
			model.addAttribute("usuariosFrecuentes", tecnicos);
		} else {
			// Visor público en pantallas de guardia/taller sin login: no exponer ni consultar catálogos privados de edición
			model.addAttribute("stockDisponibleParaTareas", List.of());
			model.addAttribute("equipos", List.of());
			model.addAttribute("equipoGenericoId", null);
			model.addAttribute("solicitantes", List.of());
			model.addAttribute("fuerosDisponibles", List.of());
			model.addAttribute("tecnicosAsignables", List.of());
			model.addAttribute("usuariosFrecuentes", List.of());
		}
	}


	private String vistaTareas(String origen) {
		return "visor".equalsIgnoreCase(origen) ? "admin/tareas-visor" : "admin/tareas";
	}

	private String redireccionTareas(String origen) {
		return "visor".equalsIgnoreCase(origen) ? "redirect:/admin/tareas/visor" : "redirect:/admin/tareas";
	}

	private List<String> fuerosParaTareas(List<UsuarioDominio> solicitantes) {
		TreeSet<String> fueros = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		fueros.addAll(fueroService.listarFueros());
		solicitantes.stream()
				.map(UsuarioDominio::fuero)
				.filter(org.springframework.util.StringUtils::hasText)
				.map(String::trim)
				.forEach(fueros::add);
		return List.copyOf(fueros);
	}

	private List<UsuarioDominio> solicitantesParaTareas() {
		java.util.List<UsuarioDominio> lista = new java.util.ArrayList<>();
		lista.add(new UsuarioDominio(
				TipoTareaTecnica.SOLICITANTE_SISTEMAS_USERNAME,
				TipoTareaTecnica.SOLICITANTE_SISTEMAS_NOMBRE,
				TipoTareaTecnica.SOLICITANTE_SISTEMAS_FUERO));
		usuarioManagementService.listarUsuariosActivosNoAdministrativos().stream()
				.map(usuario -> new UsuarioDominio(usuario.username(), usuario.nombreVisible(), usuario.fuero()))
				.forEach(lista::add);
		return List.copyOf(lista);
	}

	private void exigirPermiso(UserDetails userDetails, String permiso) {
		if (authorizationService.esAdministrador(userDetails)) {
			return;
		}
		if (!authorizationService.tienePermiso(userDetails, MODULO_TAREAS, permiso)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para operar tareas tecnicas.");
		}
	}

	private void exigirAdministradorTareas(UserDetails userDetails) {
		if (!puedeAdministrarTareas(userDetails)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un administrador puede operar tareas tecnicas.");
		}
	}

	private boolean puedeAdministrarTareas(UserDetails userDetails) {
		return userDetails != null && (
				authorizationService.esAdministrador(userDetails)
				|| authorizationService.puedeAdministrarUsuarios(userDetails)
				|| authorizationService.tienePermiso(userDetails, MODULO_TAREAS, "ADMINISTRAR")
		);
	}

	private void exigirTareaPropiaOAdministrador(UserDetails userDetails, Long tareaId) {
		if (puedeAdministrarTareas(userDetails)) {
			return;
		}
		TareaTecnicaDetalle tarea = tareaTecnicaService.obtener(tareaId);
		boolean esResponsable = tarea.responsable() != null && tarea.responsable().equalsIgnoreCase(userDetails.getUsername());
		boolean esCreador = tarea.creadoPor() != null && tarea.creadoPor().equalsIgnoreCase(userDetails.getUsername());
		if (!esResponsable && !esCreador) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"La tarea debe estar tomada por el tecnico en sesion o haber sido creada por el.");
		}
	}

	public static class TareaForm {

		private Long equipoId;

		private TipoTareaTecnica tipo = TipoTareaTecnica.INCIDENTE;

		private String codigoEquipo;

		@NotBlank
		@Size(max = 180)
		private String titulo;

		@Size(max = 1000)
		private String descripcion;

		@Size(max = 120)
		private String solicitanteUsername;

		@Size(max = 180)
		private String solicitanteNombre;

		@Size(max = 120)
		private String solicitanteFuero;

		@NotNull
		private PrioridadTareaTecnica prioridad = PrioridadTareaTecnica.MEDIA;

		@Size(max = 120)
		private String responsable;

		GuardarTareaTecnicaCommand toCommand() {
			return toCommand(null);
		}

		GuardarTareaTecnicaCommand toCommand(String creadoPor) {
			TipoTareaTecnica tipoFinal = (tipo != null) ? tipo : TipoTareaTecnica.INCIDENTE;
			String nombre = org.springframework.util.StringUtils.hasText(solicitanteNombre)
					? solicitanteNombre.trim()
					: (org.springframework.util.StringUtils.hasText(solicitanteUsername) ? solicitanteUsername.trim() : (tipoFinal.esTrabajoInterno() ? TipoTareaTecnica.SOLICITANTE_TALLER : "Sin solicitante"));
			String fuero = org.springframework.util.StringUtils.hasText(solicitanteFuero)
					? solicitanteFuero.trim()
					: (tipoFinal.esTrabajoInterno() ? TipoTareaTecnica.SOLICITANTE_TALLER : "Sin fuero informado");
			return new GuardarTareaTecnicaCommand(
					equipoId,
					titulo,
					descripcion,
					solicitanteUsername,
					nombre,
					fuero,
					prioridad,
					responsable,
					creadoPor,
					tipoFinal,
					codigoEquipo);
		}

		void aplicarReglaResponsable(String usernameActual, boolean puedeAsignarResponsable) {
			if (!puedeAsignarResponsable || (tipo != null && tipo.esTrabajoInterno() && !org.springframework.util.StringUtils.hasText(responsable))) {
				this.responsable = usernameActual;
			}
		}

		public Long getEquipoId() { return equipoId; }
		public void setEquipoId(Long equipoId) { this.equipoId = equipoId; }
		public TipoTareaTecnica getTipo() { return tipo; }
		public void setTipo(TipoTareaTecnica tipo) { this.tipo = tipo; }
		public String getCodigoEquipo() { return codigoEquipo; }
		public void setCodigoEquipo(String codigoEquipo) { this.codigoEquipo = codigoEquipo; }
		public String getTitulo() { return titulo; }
		public void setTitulo(String titulo) { this.titulo = titulo; }
		public String getDescripcion() { return descripcion; }
		public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
		public String getSolicitanteUsername() { return solicitanteUsername; }
		public void setSolicitanteUsername(String solicitanteUsername) { this.solicitanteUsername = solicitanteUsername; }
		public String getSolicitanteNombre() { return solicitanteNombre; }
		public void setSolicitanteNombre(String solicitanteNombre) { this.solicitanteNombre = solicitanteNombre; }
		public String getSolicitanteFuero() { return solicitanteFuero; }
		public void setSolicitanteFuero(String solicitanteFuero) { this.solicitanteFuero = solicitanteFuero; }
		public PrioridadTareaTecnica getPrioridad() { return prioridad; }
		public void setPrioridad(PrioridadTareaTecnica prioridad) { this.prioridad = prioridad; }
		public String getResponsable() { return responsable; }
		public void setResponsable(String responsable) { this.responsable = responsable; }
	}
}
