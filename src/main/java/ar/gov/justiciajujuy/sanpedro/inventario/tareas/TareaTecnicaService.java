package ar.gov.justiciajujuy.sanpedro.inventario.tareas;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import ar.gov.justiciajujuy.sanpedro.inventario.auditoria.AuditoriaService;
import ar.gov.justiciajujuy.sanpedro.inventario.componentes.Componente;
import ar.gov.justiciajujuy.sanpedro.inventario.componentes.ComponenteRepository;
import ar.gov.justiciajujuy.sanpedro.inventario.componentes.EstadoComparacion;
import ar.gov.justiciajujuy.sanpedro.inventario.componentes.OrigenComponente;
import ar.gov.justiciajujuy.sanpedro.inventario.equipos.Equipo;
import ar.gov.justiciajujuy.sanpedro.inventario.equipos.EquipoRepository;
import ar.gov.justiciajujuy.sanpedro.inventario.stock.EstadoStockComponente;
import ar.gov.justiciajujuy.sanpedro.inventario.stock.StockComponente;
import ar.gov.justiciajujuy.sanpedro.inventario.stock.StockComponenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TareaTecnicaService {

	public static final String EQUIPO_GENERICO_NOMBRE = "PC-GENERICA";

	private final TareaTecnicaRepository tareaTecnicaRepository;
	private final TareaTecnicaComentarioRepository comentarioRepository;
	private final TareaStockUsoRepository stockUsoRepository;
	private final EquipoRepository equipoRepository;
	private final StockComponenteRepository stockComponenteRepository;
	private final ComponenteRepository componenteRepository;
	private final AuditoriaService auditoriaService;
	private final TareaAvisoService avisoService;

	public TareaTecnicaService(
			TareaTecnicaRepository tareaTecnicaRepository,
			TareaTecnicaComentarioRepository comentarioRepository,
			TareaStockUsoRepository stockUsoRepository,
			EquipoRepository equipoRepository,
			StockComponenteRepository stockComponenteRepository,
			ComponenteRepository componenteRepository,
			AuditoriaService auditoriaService,
			TareaAvisoService avisoService) {
		this.tareaTecnicaRepository = tareaTecnicaRepository;
		this.comentarioRepository = comentarioRepository;
		this.stockUsoRepository = stockUsoRepository;
		this.equipoRepository = equipoRepository;
		this.stockComponenteRepository = stockComponenteRepository;
		this.componenteRepository = componenteRepository;
		this.auditoriaService = auditoriaService;
		this.avisoService = avisoService;
	}

	@Transactional(readOnly = true)
	public List<TareaTecnicaDetalle> buscar(EstadoTareaTecnica estado, Long equipoId, String responsable) {
		return tareaTecnicaRepository.buscar(estado, equipoId, textoOpcional(responsable)).stream()
				.map(this::toDetalle)
				.toList();
	}

	@Transactional(readOnly = true)
	public TareaTecnicaDetalle obtener(Long id) {
		return tareaTecnicaRepository.findById(id)
				.map(this::toDetalle)
				.orElseThrow(() -> new TareaTecnicaNoEncontradaException(id));
	}

	public long contar() {
		return tareaTecnicaRepository.count();
	}

	@Transactional(readOnly = true)
	public ResumenTareas resumenDelDia() {
		LocalDate hoy = LocalDate.now();
		LocalDateTime inicio = hoy.atStartOfDay();
		LocalDateTime fin = hoy.plusDays(1).atStartOfDay();
		long pendientes = tareaTecnicaRepository.countByEstado(EstadoTareaTecnica.PENDIENTE);
		long enProceso = tareaTecnicaRepository.countByEstado(EstadoTareaTecnica.EN_PROCESO);
		long realizadasHoy = tareaTecnicaRepository.countByCerradoEnBetween(inicio, fin);
		long creadasHoy = tareaTecnicaRepository.countByCreadoEnBetween(inicio, fin);
		return new ResumenTareas(tareaTecnicaRepository.count(), pendientes, enProceso, realizadasHoy, creadasHoy);
	}

	@Transactional(readOnly = true)
	public List<TareaComentarioDetalle> comentarios(Long tareaId) {
		validarExistencia(tareaId);
		return comentarioRepository.findByTareaIdOrderByCreadoEnDescIdDesc(tareaId).stream()
				.map(this::toComentarioDetalle)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<StockDisponibleDetalle> stockDisponibleParaTareas() {
		return stockComponenteRepository.findByActivoTrueOrderByTipoAscDescripcionAsc().stream()
				.filter(componente -> componente.getEstado() == EstadoStockComponente.DISPONIBLE)
				.map(this::toStockDisponibleDetalle)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<TareaStockUsoDetalle> stockUsado(Long tareaId) {
		validarExistencia(tareaId);
		return stockUsoRepository.findByTareaIdOrderByCreadoEnDescIdDesc(tareaId).stream()
				.map(this::toStockUsoDetalle)
				.toList();
	}

	@Transactional
	public TareaTecnicaDetalle crear(GuardarTareaTecnicaCommand command) {
		TareaTecnica tarea = new TareaTecnica(textoRequerido(command.titulo(), "titulo"));
		tarea.actualizarDatos(
				buscarEquipoParaTarea(command.equipoId()),
				textoRequerido(command.titulo(), "titulo"),
				textoOpcional(command.descripcion()),
				textoRequerido(command.solicitanteUsername(), "solicitante"),
				textoRequerido(command.solicitanteNombre(), "solicitanteNombre"),
				textoRequerido(command.solicitanteFuero(), "solicitanteFuero"),
				command.prioridad() == null ? PrioridadTareaTecnica.MEDIA : command.prioridad(),
				textoOpcional(command.responsable()));
		tarea.marcarCreadoPor(textoOpcional(command.creadoPor()));
		TareaTecnica guardada = tareaTecnicaRepository.save(tarea);
		auditoriaService.registrar("TAREAS", "CREAR", "TareaTecnica", guardada.getId(),
				"Tarea tecnica creada: " + guardada.getTitulo() + ".");
		avisoService.registrarCreacion(guardada);
		return toDetalle(guardada);
	}

	@Transactional
	public TareaTecnicaDetalle actualizar(Long id, GuardarTareaTecnicaCommand command) {
		TareaTecnica tarea = tareaTecnicaRepository.findById(id)
				.orElseThrow(() -> new TareaTecnicaNoEncontradaException(id));
		tarea.actualizarDatos(
				buscarEquipoParaTarea(command.equipoId()),
				textoRequerido(command.titulo(), "titulo"),
				textoOpcional(command.descripcion()),
				textoRequerido(command.solicitanteUsername(), "solicitante"),
				textoRequerido(command.solicitanteNombre(), "solicitanteNombre"),
				textoRequerido(command.solicitanteFuero(), "solicitanteFuero"),
				command.prioridad() == null ? PrioridadTareaTecnica.MEDIA : command.prioridad(),
				textoOpcional(command.responsable()));
		auditoriaService.registrar("TAREAS", "ACTUALIZAR", "TareaTecnica", tarea.getId(),
				"Tarea tecnica " + tarea.getId() + " actualizada.");
		return toDetalle(tarea);
	}

	@Transactional
	public TareaTecnicaDetalle tomar(Long id, String responsable) {
		TareaTecnica tarea = tareaTecnicaRepository.buscarParaTomar(id)
				.orElseThrow(() -> new TareaTecnicaNoEncontradaException(id));
		if (tarea.getEstado() == EstadoTareaTecnica.CERRADA || tarea.getEstado() == EstadoTareaTecnica.CANCELADA) {
			throw new TareaTecnicaFinalizadaException(id);
		}
		String responsableNormalizado = textoRequerido(responsable, "responsable");
		if (StringUtils.hasText(tarea.getResponsable()) && !tarea.getResponsable().equalsIgnoreCase(responsableNormalizado)) {
			throw new TareaTecnicaYaAsignadaException(id, tarea.getResponsable());
		}
		tarea.tomar(responsableNormalizado);
		auditoriaService.registrar("TAREAS", "TOMAR", "TareaTecnica", tarea.getId(),
				"Tarea tecnica " + tarea.getId() + " tomada por " + responsableNormalizado + ".");
		avisoService.registrarAsignacion(tarea, responsableNormalizado);
		return toDetalle(tarea);
	}

	@Transactional
	public TareaTecnicaDetalle reasignarEquipo(Long id, Long equipoDestinoId, String usuario) {
		TareaTecnica tarea = tareaTecnicaRepository.findById(id)
				.orElseThrow(() -> new TareaTecnicaNoEncontradaException(id));
		Equipo equipoDestino = buscarEquipoOpcional(equipoDestinoId);
		if (EQUIPO_GENERICO_NOMBRE.equalsIgnoreCase(equipoDestino.getNombre())) {
			throw new IllegalArgumentException("La tarea ya se encuentra en la PC generica auxiliar.");
		}
		tarea.reasignarEquipo(equipoDestino);
		auditoriaService.registrar("TAREAS", "REASIGNAR_EQUIPO", "TareaTecnica", tarea.getId(),
				"Tarea tecnica " + tarea.getId() + " reasignada al equipo " + equipoDestino.getNombre()
						+ " por " + textoOpcional(usuario) + ".");
		return toDetalle(tarea);
	}

	@Transactional
	public TareaTecnicaDetalle cambiarEstado(Long id, CambiarEstadoTareaCommand command) {
		return cambiarEstado(id, command, null);
	}

	@Transactional
	public TareaTecnicaDetalle cambiarEstado(Long id, CambiarEstadoTareaCommand command, String autor) {
		TareaTecnica tarea = tareaTecnicaRepository.findById(id)
				.orElseThrow(() -> new TareaTecnicaNoEncontradaException(id));
		EstadoTareaTecnica estado = command.estado() == null ? EstadoTareaTecnica.PENDIENTE : command.estado();
		tarea.cambiarEstado(estado, textoOpcional(command.observacionesCierre()));
		auditoriaService.registrar("TAREAS", "CAMBIAR_ESTADO", "TareaTecnica", tarea.getId(),
				"Tarea tecnica " + tarea.getId() + " cambio a " + estado + ".");
		avisoService.registrarCambioEstado(tarea, textoOpcional(autor));
		return toDetalle(tarea);
	}

	@Transactional
	public TareaComentarioDetalle comentar(Long id, AgregarComentarioTareaCommand command) {
		TareaTecnica tarea = tareaTecnicaRepository.findById(id)
				.orElseThrow(() -> new TareaTecnicaNoEncontradaException(id));
		TareaTecnicaComentario comentario = comentarioRepository.save(new TareaTecnicaComentario(
				tarea,
				textoRequerido(command.autor(), "autor"),
				textoRequerido(command.comentario(), "comentario")));
		auditoriaService.registrar("TAREAS", "COMENTAR", "TareaTecnica", tarea.getId(),
				"Comentario agregado a tarea tecnica " + tarea.getId() + ".");
		avisoService.registrarComentario(tarea, comentario.getAutor());
		return toComentarioDetalle(comentario);
	}

	@Transactional
	public TareaStockUsoDetalle registrarUsoStock(Long id, RegistrarUsoStockCommand command) {
		TareaTecnica tarea = tareaTecnicaRepository.findById(id)
				.orElseThrow(() -> new TareaTecnicaNoEncontradaException(id));
		StockComponente componente = stockComponenteRepository.findById(command.stockComponenteId())
				.orElseThrow(() -> new StockComponenteNoEncontradoException(command.stockComponenteId()));
		if (!componente.isActivo() || componente.getEstado() != EstadoStockComponente.DISPONIBLE) {
			throw new StockComponenteNoDisponibleParaTareaException(command.stockComponenteId());
		}
		componente.reservar();
		TareaStockUso uso = stockUsoRepository.save(new TareaStockUso(
				tarea,
				componente,
				textoRequerido(command.registradoPor(), "registradoPor"),
				textoOpcional(command.observacion())));
		auditoriaService.registrar("TAREAS", "USAR_STOCK", "TareaTecnica", tarea.getId(),
				"Tarea tecnica " + tarea.getId() + " reservo stock #" + componente.getId() + " (" + componente.getDescripcion() + ").");
		avisoService.registrarStockUsado(tarea, command.registradoPor());
		return toStockUsoDetalle(uso);
	}

	@Transactional
	public InstalarEnEquipoDetalle instalarEnEquipo(Long tareaId, Long usoId, String instaladoPor) {
		TareaStockUso uso = stockUsoRepository.findById(usoId)
				.orElseThrow(() -> new StockUsoNoEncontradoException(usoId));
		if (!uso.getTarea().getId().equals(tareaId)) {
			throw new StockUsoNoEncontradoException(usoId);
		}
		StockComponente componente = uso.getStockComponente();
		if (componente.getEstado() != EstadoStockComponente.RESERVADO) {
			throw new StockComponenteNoReservadoException(componente.getId());
		}
		Equipo equipo = uso.getTarea().getEquipo();
		if (equipo == null || EQUIPO_GENERICO_NOMBRE.equalsIgnoreCase(equipo.getNombre())) {
			throw new TareaEquipoGenericoException(tareaId);
		}
		// Marcar stock como ASIGNADO (instalado en equipo real)
		componente.asignar();
		// Crear el componente oficial en el equipo
		Componente compInstalado = new Componente(
				equipo,
				componente.getTipo(),
				OrigenComponente.STOCK,
				EstadoComparacion.ESPERADO,
				componente.getDescripcion());
		compInstalado.actualizar(
				componente.getTipo(),
				OrigenComponente.STOCK,
				EstadoComparacion.ESPERADO,
				componente.getDescripcion(),
				componente.getMarca(),
				componente.getModelo(),
				componente.getSerial(),
				componente.getCapacidad(),
				componente.getRemito(),
				componente.getOrdenCompra(),
				componente.getProveedor(),
				null,
				"Instalado desde tarea #" + tareaId + (StringUtils.hasText(uso.getObservacion()) ? " - " + uso.getObservacion() : ""),
				true);
		Componente guardado = componenteRepository.save(compInstalado);
		auditoriaService.registrar("TAREAS", "INSTALAR_EN_EQUIPO", "TareaTecnica", tareaId,
				"Stock #" + componente.getId() + " (" + componente.getDescripcion() + ") instalado en equipo '"
						+ equipo.getNombre() + "' por " + textoOpcional(instaladoPor) + ". Componente #" + guardado.getId() + " creado.");
		auditoriaService.registrar("COMPONENTES", "CREAR", "Componente", guardado.getId(),
				"Componente " + guardado.getTipo() + " creado para equipo " + equipo.getNombre()
						+ " desde tarea #" + tareaId + " (origen STOCK).");
		return new InstalarEnEquipoDetalle(
				guardado.getId(),
				equipo.getId(),
				equipo.getNombre(),
				componente.getId(),
				componente.getDescripcion());
	}

	@Transactional
	public void eliminar(Long id) {
		TareaTecnica tarea = tareaTecnicaRepository.findById(id)
				.orElseThrow(() -> new TareaTecnicaNoEncontradaException(id));
		String titulo = tarea.getTitulo();
		comentarioRepository.deleteByTareaId(id);
		tareaTecnicaRepository.delete(tarea);
		auditoriaService.registrar("TAREAS", "ELIMINAR", "TareaTecnica", id,
				"Tarea técnica " + id + " (" + titulo + ") eliminada.");
	}

	private Equipo buscarEquipoOpcional(Long equipoId) {
		if (equipoId == null) {
			return null;
		}
		return equipoRepository.findById(equipoId)
				.orElseThrow(() -> new EquipoNoEncontradoException(equipoId));
	}

	private Equipo buscarEquipoParaTarea(Long equipoId) {
		if (equipoId != null) {
			return buscarEquipoOpcional(equipoId);
		}
		return equipoRepository.findByNombreIgnoreCase(EQUIPO_GENERICO_NOMBRE)
				.orElseGet(() -> equipoRepository.save(new Equipo(EQUIPO_GENERICO_NOMBRE, "Sin fuero informado")));
	}

	private void validarExistencia(Long tareaId) {
		if (!tareaTecnicaRepository.existsById(tareaId)) {
			throw new TareaTecnicaNoEncontradaException(tareaId);
		}
	}

	private TareaTecnicaDetalle toDetalle(TareaTecnica tarea) {
		Equipo equipo = tarea.getEquipo();
		return new TareaTecnicaDetalle(
				tarea.getId(),
				equipo == null ? null : equipo.getId(),
				equipo == null ? null : equipo.getNombre(),
				tarea.getTitulo(),
				tarea.getDescripcion(),
				tarea.getSolicitanteUsername(),
				tarea.getSolicitanteNombre(),
				tarea.getSolicitanteFuero(),
				tarea.getEstado(),
				tarea.getPrioridad(),
				tarea.getResponsable(),
				tarea.getCreadoPor(),
				tarea.getObservacionesCierre(),
				tarea.getCreadoEn(),
				tarea.getCerradoEn());
	}

	private TareaComentarioDetalle toComentarioDetalle(TareaTecnicaComentario comentario) {
		return new TareaComentarioDetalle(
				comentario.getId(),
				comentario.getTarea().getId(),
				comentario.getAutor(),
				comentario.getComentario(),
				comentario.getCreadoEn());
	}

	private StockDisponibleDetalle toStockDisponibleDetalle(StockComponente componente) {
		return new StockDisponibleDetalle(
				componente.getId(),
				componente.getTipo().name(),
				componente.getEstado().name(),
				componente.getDescripcion(),
				componente.getMarca(),
				componente.getModelo(),
				componente.getSerial(),
				componente.getCapacidad(),
				componente.getUbicacion());
	}

	private TareaStockUsoDetalle toStockUsoDetalle(TareaStockUso uso) {
		StockComponente componente = uso.getStockComponente();
		return new TareaStockUsoDetalle(
				uso.getId(),
				componente.getId(),
				componente.getTipo().name(),
				componente.getDescripcion(),
				componente.getMarca(),
				componente.getModelo(),
				componente.getSerial(),
				componente.getCapacidad(),
				uso.getRegistradoPor(),
				uso.getObservacion(),
				uso.getCreadoEn());
	}

	private String textoOpcional(String valor) {
		return StringUtils.hasText(valor) ? valor.trim() : null;
	}

	private String textoRequerido(String valor, String campo) {
		if (!StringUtils.hasText(valor)) {
			throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
		}
		return valor.trim();
	}

	public record GuardarTareaTecnicaCommand(
			Long equipoId,
			String titulo,
			String descripcion,
			String solicitanteUsername,
			String solicitanteNombre,
			String solicitanteFuero,
			PrioridadTareaTecnica prioridad,
			String responsable,
			String creadoPor) {
	}

	public record CambiarEstadoTareaCommand(
			EstadoTareaTecnica estado,
			String observacionesCierre) {
	}

	public record AgregarComentarioTareaCommand(
			String autor,
			String comentario) {
	}

	public record RegistrarUsoStockCommand(
			Long stockComponenteId,
			String registradoPor,
			String observacion) {
	}

	public record InstalarEnEquipoDetalle(
			Long componenteId,
			Long equipoId,
			String equipoNombre,
			Long stockComponenteId,
			String descripcion) {
	}

	public record TareaTecnicaDetalle(
			Long id,
			Long equipoId,
			String equipoNombre,
			String titulo,
			String descripcion,
			String solicitanteUsername,
			String solicitanteNombre,
			String solicitanteFuero,
			EstadoTareaTecnica estado,
			PrioridadTareaTecnica prioridad,
			String responsable,
			String creadoPor,
			String observacionesCierre,
			LocalDateTime creadoEn,
			LocalDateTime cerradoEn) {
	}

	public record TareaComentarioDetalle(
			Long id,
			Long tareaId,
			String autor,
			String comentario,
			LocalDateTime creadoEn) {
	}

	public record StockDisponibleDetalle(
			Long id,
			String tipo,
			String estado,
			String descripcion,
			String marca,
			String modelo,
			String serial,
			String capacidad,
			String ubicacion) {
	}

	public record TareaStockUsoDetalle(
			Long id,
			Long stockComponenteId,
			String tipo,
			String descripcion,
			String marca,
			String modelo,
			String serial,
			String capacidad,
			String registradoPor,
			String observacion,
			LocalDateTime creadoEn) {
	}

	public record ResumenTareas(
			long total,
			long pendientes,
			long enProceso,
			long realizadasHoy,
			long creadasHoy) {
	}

	public static class TareaTecnicaNoEncontradaException extends RuntimeException {
		public TareaTecnicaNoEncontradaException(Long id) {
			super("Tarea tecnica no encontrada: " + id);
		}
	}

	public static class TareaTecnicaYaAsignadaException extends RuntimeException {
		public TareaTecnicaYaAsignadaException(Long id, String responsable) {
			super("Tarea tecnica " + id + " ya asignada a " + responsable + ".");
		}
	}

	@org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CONFLICT)
	public static class TareaTecnicaFinalizadaException extends RuntimeException {
		public TareaTecnicaFinalizadaException(Long id) {
			super("La tarea " + id + " esta finalizada y no se puede tomar.");
		}
	}

	public static class EquipoNoEncontradoException extends RuntimeException {
		public EquipoNoEncontradoException(Long id) {
			super("Equipo no encontrado: " + id);
		}
	}

	public static class StockComponenteNoEncontradoException extends RuntimeException {
		public StockComponenteNoEncontradoException(Long id) {
			super("Componente de stock no encontrado: " + id);
		}
	}

	public static class StockComponenteNoDisponibleParaTareaException extends RuntimeException {
		public StockComponenteNoDisponibleParaTareaException(Long id) {
			super("Componente de stock no disponible para tarea: " + id);
		}
	}

	public static class StockComponenteNoReservadoException extends RuntimeException {
		public StockComponenteNoReservadoException(Long id) {
			super("El componente de stock " + id + " no está en estado RESERVADO y no puede instalarse.");
		}
	}

	public static class StockUsoNoEncontradoException extends RuntimeException {
		public StockUsoNoEncontradoException(Long id) {
			super("Uso de stock no encontrado: " + id);
		}
	}

	@org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
	public static class TareaEquipoGenericoException extends RuntimeException {
		public TareaEquipoGenericoException(Long tareaId) {
			super("La tarea " + tareaId + " está asociada a la PC genérica. Reasignela a un equipo real antes de instalar.");
		}
	}
}
