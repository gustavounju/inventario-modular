package ar.gov.justiciajujuy.sanpedro.inventario.tareas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import ar.gov.justiciajujuy.sanpedro.inventario.auditoria.AuditoriaService;
import ar.gov.justiciajujuy.sanpedro.inventario.equipos.Equipo;
import ar.gov.justiciajujuy.sanpedro.inventario.equipos.EquipoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Motor de Conciliación y Resolución de Entidades: vincula tareas huérfanas
 * (asignadas a PC-GENERICA) con sus equipos físicos reales del inventario
 * a partir de la correlación del usuario solicitante.
 */
@Service
public class TareaConciliacionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TareaConciliacionService.class);

	private final TareaTecnicaRepository tareaTecnicaRepository;
	private final EquipoRepository equipoRepository;
	private final AuditoriaService auditoriaService;

	public TareaConciliacionService(
			TareaTecnicaRepository tareaTecnicaRepository,
			EquipoRepository equipoRepository,
			AuditoriaService auditoriaService) {
		this.tareaTecnicaRepository = tareaTecnicaRepository;
		this.equipoRepository = equipoRepository;
		this.auditoriaService = auditoriaService;
	}

	public record PropuestaConciliacion(
			Long tareaId,
			String tareaTitulo,
			String solicitanteUsername,
			String solicitanteNombre,
			String solicitanteFuero,
			String equipoActual,
			Long equipoSugeridoId,
			String equipoSugeridoNombre,
			String equipoSugeridoFuero,
			String equipoSugeridoUbicacion,
			String criterioMatch
	) {}

	public record ResumenConciliacion(
			int totalGenericas,
			int totalVinculables,
			int totalSinCoincidencia,
			List<PropuestaConciliacion> propuestas
	) {}

	public record ResultadoConciliacion(
			int totalAplicadas,
			String mensaje
	) {}

	/**
	 * Simula y analiza todas las tareas con PC genérica para encontrar su equipo real.
	 * No modifica la base de datos (Dry Run).
	 */
	@Transactional(readOnly = true)
	public ResumenConciliacion analizar() {
		List<TareaTecnica> tareasGenericas = tareaTecnicaRepository.buscarTareasConEquipoGenerico();
		List<Equipo> equiposValidos = equipoRepository.findAllConUltimoUsuarioValido();

		List<PropuestaConciliacion> propuestas = new ArrayList<>();
		int totalGenericas = 0;
		int sinCoincidencia = 0;

		for (TareaTecnica tarea : tareasGenericas) {
			if (tarea.getTipo() != null && tarea.getTipo().esTrabajoInterno()) {
				// No vincular tareas de armado o taller interno a computadoras de oficina.
				continue;
			}
			totalGenericas++;

			String solicitanteUser = normalizar(tarea.getSolicitanteUsername());
			String solicitanteNom = normalizar(tarea.getSolicitanteNombre());
			String solicitanteFuero = tarea.getSolicitanteFuero() != null ? tarea.getSolicitanteFuero().trim() : "";

			if (!StringUtils.hasText(solicitanteUser) && !StringUtils.hasText(solicitanteNom)) {
				sinCoincidencia++;
				continue;
			}

			// Buscar candidatos en equipos
			List<Equipo> candidatos = equiposValidos.stream()
					.filter(e -> coincideUsuario(e.getUltimoUsuario(), solicitanteUser, solicitanteNom))
					.sorted(Comparator.comparing(Equipo::getId, Comparator.nullsLast(Comparator.reverseOrder())))
					.toList();

			if (candidatos.isEmpty()) {
				sinCoincidencia++;
				continue;
			}

			Equipo seleccionado;
			String criterio;

			if (candidatos.size() == 1) {
				seleccionado = candidatos.get(0);
				criterio = "Coincidencia exacta 1 a 1";
			} else {
				// Múltiples PCs: intentar desempate por Fuero
				List<Equipo> mismoFuero = candidatos.stream()
						.filter(e -> StringUtils.hasText(solicitanteFuero)
								&& StringUtils.hasText(e.getFuero())
								&& e.getFuero().equalsIgnoreCase(solicitanteFuero))
						.toList();

				if (mismoFuero.size() == 1) {
					seleccionado = mismoFuero.get(0);
					criterio = "Desempate por coincidencia de Fuero (" + solicitanteFuero + ")";
				} else if (mismoFuero.size() > 1) {
					seleccionado = mismoFuero.get(0); // el más reciente de ese fuero
					criterio = "Múltiples PCs en fuero: asignada la más reciente";
				} else {
					seleccionado = candidatos.get(0); // el más reciente general
					criterio = "Múltiples PCs: asignada la más reciente";
				}
			}

			String pcActualNombre = tarea.getEquipo() != null ? tarea.getEquipo().getNombre() : "PC-GENERICA";

			propuestas.add(new PropuestaConciliacion(
					tarea.getId(),
					tarea.getTitulo(),
					tarea.getSolicitanteUsername() != null ? tarea.getSolicitanteUsername() : "-",
					tarea.getSolicitanteNombre() != null ? tarea.getSolicitanteNombre() : "-",
					tarea.getSolicitanteFuero() != null ? tarea.getSolicitanteFuero() : "-",
					pcActualNombre,
					seleccionado.getId(),
					seleccionado.getNombre(),
					seleccionado.getFuero() != null ? seleccionado.getFuero() : "-",
					seleccionado.getUbicacion() != null ? seleccionado.getUbicacion() : "-",
					criterio
			));
		}

		return new ResumenConciliacion(
				totalGenericas,
				propuestas.size(),
				sinCoincidencia,
				propuestas
		);
	}

	/**
	 * Aplica la vinculación de equipos propuesta en la base de datos y genera auditoría.
	 */
	@Transactional
	public ResultadoConciliacion aplicar(List<Long> tareaIdsSeleccionadas) {
		ResumenConciliacion analisis = analizar();
		Set<Long> filtroIds = (tareaIdsSeleccionadas != null && !tareaIdsSeleccionadas.isEmpty())
				? tareaIdsSeleccionadas.stream().filter(Objects::nonNull).collect(Collectors.toSet())
				: null;

		int aplicadas = 0;
		for (PropuestaConciliacion propuesta : analisis.propuestas()) {
			if (filtroIds != null && !filtroIds.contains(propuesta.tareaId())) {
				continue;
			}

			TareaTecnica tarea = tareaTecnicaRepository.findById(propuesta.tareaId()).orElse(null);
			Equipo nuevoEquipo = equipoRepository.findById(propuesta.equipoSugeridoId()).orElse(null);

			if (tarea != null && nuevoEquipo != null) {
				String anterior = tarea.getEquipo() != null ? tarea.getEquipo().getNombre() : "PC-GENERICA";
				tarea.reasignarEquipo(nuevoEquipo);
				tareaTecnicaRepository.save(tarea);

				auditoriaService.registrar(
						"TAREAS",
						"CONCILIACION_AUTOMATICA",
						"TareaTecnica",
						tarea.getId(),
						String.format("Equipo reasignado de %s a %s (%s) para solicitante '%s'.",
								anterior, nuevoEquipo.getNombre(), propuesta.criterioMatch(), propuesta.solicitanteUsername())
				);
				aplicadas++;
			}
		}

		String mensaje = aplicadas > 0
				? String.format("Se vincularon exitosamente %d tareas a sus equipos correspondientes del inventario.", aplicadas)
				: "No se aplicaron cambios (no se seleccionaron tareas o ya se encontraban vinculadas).";

		LOGGER.info("Conciliación de equipos completada: {} tareas reasignadas.", aplicadas);
		return new ResultadoConciliacion(aplicadas, mensaje);
	}

	private boolean coincideUsuario(String equipoUsuario, String solicitanteUser, String solicitanteNom) {
		if (!StringUtils.hasText(equipoUsuario)) {
			return false;
		}
		String eqNorm = normalizar(equipoUsuario);
		if (!StringUtils.hasText(eqNorm)) {
			return false;
		}

		if (StringUtils.hasText(solicitanteUser) && eqNorm.equals(solicitanteUser)) {
			return true;
		}
		if (StringUtils.hasText(solicitanteNom) && eqNorm.equals(solicitanteNom)) {
			return true;
		}
		// Si contiene o está contenido cuando tiene longitud significativa (>= 4 caracteres)
		if (StringUtils.hasText(solicitanteUser) && solicitanteUser.length() >= 4
				&& (eqNorm.contains(solicitanteUser) || solicitanteUser.contains(eqNorm))) {
			return true;
		}
		return false;
	}

	public static String normalizar(String valor) {
		if (valor == null) {
			return "";
		}
		String s = valor.trim().toLowerCase();
		int slash = s.indexOf('\\');
		if (slash >= 0 && slash + 1 < s.length()) {
			s = s.substring(slash + 1).trim();
		}
		int at = s.indexOf('@');
		if (at > 0) {
			s = s.substring(0, at).trim();
		}
		return s;
	}
}

