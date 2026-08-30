package ar.gov.justiciajujuy.sanpedro.inventario.equipos;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EquipoService {

	private final EquipoRepository equipoRepository;
	private final Clock clock;

	@Autowired
	public EquipoService(EquipoRepository equipoRepository) {
		this(equipoRepository, Clock.systemDefaultZone());
	}

	EquipoService(EquipoRepository equipoRepository, Clock clock) {
		this.equipoRepository = equipoRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public EquipoPagina listar(String query, int page, int pageSize) {
		int pagina = Math.max(0, page);
		int tamano = Math.max(1, Math.min(pageSize, 100));
		String filtro = StringUtils.hasText(query) ? query.trim() : null;
		Page<Equipo> resultado = equipoRepository.buscar(
				filtro,
				PageRequest.of(pagina, tamano, Sort.by("nombre").ascending()));
		return new EquipoPagina(
				resultado.getContent().stream().map(this::toResumen).toList(),
				new Paginacion(resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(), resultado.getTotalPages()));
	}

	@Transactional(readOnly = true)
	public EquipoDetalle obtener(Long id) {
		return equipoRepository.findById(id)
				.map(this::toDetalle)
				.orElseThrow(() -> new EquipoNoEncontradoException(id));
	}

	@Transactional
	public EquipoDetalle registrarInventario(ReporteInventarioCommand command) {
		String nombre = normalizarNombre(command.nombre());
		Equipo equipo = equipoRepository.findByNombreIgnoreCase(nombre)
				.orElseGet(() -> new Equipo(nombre, command.fuero().trim()));
		equipo.actualizarDesdeReporte(
				textoOpcional(command.ultimoUsuario()),
				command.fuero().trim(),
				textoOpcional(command.ip()),
				textoOpcional(command.sistemaOperativo()),
				textoOpcional(command.procesador()),
				command.ramMb(),
				textoOpcional(command.impresora()),
				command.activo(),
				LocalDateTime.now(clock));
		return toDetalle(equipoRepository.save(equipo));
	}

	private EquipoResumen toResumen(Equipo equipo) {
		return new EquipoResumen(
				equipo.getId(),
				equipo.getNombre(),
				equipo.getUltimoUsuario(),
				equipo.getFuero(),
				equipo.getIp(),
				equipo.getSistemaOperativo(),
				equipo.getMonitoreo(),
				equipo.isActivo());
	}

	private EquipoDetalle toDetalle(Equipo equipo) {
		return new EquipoDetalle(
				equipo.getId(),
				equipo.getNombre(),
				equipo.getUltimoUsuario(),
				equipo.getFuero(),
				equipo.getIp(),
				equipo.getSistemaOperativo(),
				equipo.getProcesador(),
				equipo.getRamMb(),
				equipo.getImpresora(),
				equipo.getMonitoreo(),
				equipo.isActivo(),
				equipo.getUltimoReporteEn());
	}

	private String normalizarNombre(String nombre) {
		return nombre.trim().toUpperCase();
	}

	private String textoOpcional(String valor) {
		return StringUtils.hasText(valor) ? valor.trim() : null;
	}

	public record ReporteInventarioCommand(
			String nombre,
			String ultimoUsuario,
			String fuero,
			String ip,
			String sistemaOperativo,
			String procesador,
			Integer ramMb,
			String impresora,
			boolean activo) {
	}

	public record EquipoPagina(List<EquipoResumen> equipos, Paginacion paginacion) {
	}

	public record Paginacion(int page, int pageSize, long totalItems, int totalPages) {
	}

	public record EquipoResumen(
			Long id,
			String nombre,
			String ultimoUsuario,
			String fuero,
			String ip,
			String sistemaOperativo,
			String monitoreo,
			boolean activo) {
	}

	public record EquipoDetalle(
			Long id,
			String nombre,
			String ultimoUsuario,
			String fuero,
			String ip,
			String sistemaOperativo,
			String procesador,
			Integer ramMb,
			String impresora,
			String monitoreo,
			boolean activo,
			LocalDateTime ultimoReporteEn) {
	}

	public static class EquipoNoEncontradoException extends RuntimeException {

		public EquipoNoEncontradoException(Long id) {
			super("Equipo no encontrado: " + id);
		}
	}
}
