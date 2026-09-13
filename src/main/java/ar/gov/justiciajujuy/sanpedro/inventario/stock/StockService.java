package ar.gov.justiciajujuy.sanpedro.inventario.stock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import ar.gov.justiciajujuy.sanpedro.inventario.auditoria.AuditoriaService;
import ar.gov.justiciajujuy.sanpedro.inventario.componentes.Componente;
import ar.gov.justiciajujuy.sanpedro.inventario.componentes.ComponenteRepository;
import ar.gov.justiciajujuy.sanpedro.inventario.componentes.TipoComponente;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StockService {

	private final StockComponenteRepository stockComponenteRepository;
	private final ComponenteRepository componenteRepository;
	private final AuditoriaService auditoriaService;

	public StockService(StockComponenteRepository stockComponenteRepository, ComponenteRepository componenteRepository,
			AuditoriaService auditoriaService) {
		this.stockComponenteRepository = stockComponenteRepository;
		this.componenteRepository = componenteRepository;
		this.auditoriaService = auditoriaService;
	}

	@Transactional
	public List<StockComponenteDetalle> listarDisponiblesYActivos() {
		var componentes = stockComponenteRepository.findByActivoTrueOrderByTipoAscDescripcionAsc();
		// El stock no guarda equipo_id: el vinculo operativo se reconstruye por serial.
		// El vinculo activo manda sobre el estado visible: evita piezas "disponibles"
		// que ya estan instaladas, y libera asignaciones viejas sin equipo activo.
		componentes.forEach(this::sincronizarEstadoConVinculoActivo);
		return componentes.stream()
				.map(this::toDetalle)
				.toList();
	}

	@Transactional
	public StockComponenteDetalle crear(GuardarStockComponenteCommand command) {
		StockComponente componente = new StockComponente(command.tipo(), textoRequerido(command.descripcion(), "descripcion"));
		aplicarCampos(componente, command);
		componente.registrarIngreso(textoOpcional(command.ingresadoPor()));
		StockComponente guardado = stockComponenteRepository.save(componente);
		auditoriaService.registrar("STOCK", "CREAR", "StockComponente", guardado.getId(),
				"Componente de stock " + guardado.getTipo() + " creado con estado " + guardado.getEstado()
						+ (StringUtils.hasText(guardado.getIngresadoPor()) ? " por " + guardado.getIngresadoPor() : "") + ".");
		return toDetalle(guardado);
	}

	@Transactional
	public List<StockComponenteDetalle> crearPendientesDesdeCodigos(CrearStockPendienteLoteCommand command) {
		Set<String> codigos = command.codigos().stream()
				.map(this::textoOpcional)
				.filter(StringUtils::hasText)
				.collect(Collectors.toCollection(java.util.LinkedHashSet::new));
		if (codigos.isEmpty()) {
			throw new LoteStockSinDatosException();
		}
		List<StockComponente> componentes = new ArrayList<>();
		for (String codigo : codigos) {
			StockComponente componente = new StockComponente(TipoComponente.PENDIENTE, "Pendiente de completar - " + codigo);
			componente.actualizar(
					TipoComponente.PENDIENTE,
					EstadoStockComponente.DISPONIBLE,
					"Pendiente de completar - " + codigo,
					null,
					null,
					codigo,
					null,
					null,
					null,
					null,
					null,
					"Lote escaneado desde celular. Completar datos desde Stock web.",
					true);
			componente.registrarIngreso(textoOpcional(command.ingresadoPor()));
			componente.marcarPendiente();
			componentes.add(componente);
		}
		List<StockComponente> guardados = stockComponenteRepository.saveAll(componentes);
		auditoriaService.registrar("STOCK", "CREAR_LOTE_PENDIENTE", "StockComponente", null,
				"Escaneo rapido creo " + guardados.size() + " componentes pendientes de completar.");
		return guardados.stream().map(this::toDetalle).toList();
	}

	@Transactional
	public StockComponenteDetalle actualizar(Long id, GuardarStockComponenteCommand command) {
		StockComponente componente = stockComponenteRepository.findById(id)
				.orElseThrow(() -> new StockComponenteNoEncontradoException(id));
		aplicarCampos(componente, command);
		StockComponente guardado = stockComponenteRepository.save(componente);
		auditoriaService.registrar("STOCK", "ACTUALIZAR", "StockComponente", guardado.getId(),
				"Componente de stock " + guardado.getTipo() + " actualizado con estado " + guardado.getEstado() + ".");
		return toDetalle(guardado);
	}

	@Transactional
	public int actualizarDatosAdministrativosEnLote(ActualizarStockLoteCommand command) {
		Set<Long> ids = command.ids().stream()
				.filter(id -> id != null && id > 0)
				.collect(Collectors.toCollection(java.util.LinkedHashSet::new));
		if (ids.isEmpty()) {
			throw new LoteStockSinSeleccionException();
		}
		String remito = textoOpcional(command.remito());
		String ordenCompra = textoOpcional(command.ordenCompra());
		String proveedor = textoOpcional(command.proveedor());
		String descripcion = textoOpcional(command.descripcion());
		String marca = textoOpcional(command.marca());
		String modelo = textoOpcional(command.modelo());
		String capacidad = textoOpcional(command.capacidad());
		String observaciones = textoOpcional(command.observaciones());
		boolean hayTipo = command.tipo() != null;
		boolean hayEstado = command.estado() != null;
		if (!hayTipo && !hayEstado && !StringUtils.hasText(descripcion) && !StringUtils.hasText(marca)
				&& !StringUtils.hasText(modelo) && !StringUtils.hasText(capacidad) && !StringUtils.hasText(remito)
				&& !StringUtils.hasText(ordenCompra) && !StringUtils.hasText(proveedor) && !StringUtils.hasText(observaciones)) {
			throw new LoteStockSinDatosException();
		}
		List<StockComponente> componentes = stockComponenteRepository.findAllById(ids);
		if (componentes.size() != ids.size()) {
			throw new LoteStockConComponentesInvalidosException();
		}
		for (StockComponente componente : componentes) {
			componente.actualizar(
					hayTipo ? command.tipo() : componente.getTipo(),
					hayEstado ? command.estado() : componente.getEstado(),
					StringUtils.hasText(descripcion) ? descripcion : componente.getDescripcion(),
					StringUtils.hasText(marca) ? marca : componente.getMarca(),
					StringUtils.hasText(modelo) ? modelo : componente.getModelo(),
					componente.getSerial(),
					StringUtils.hasText(capacidad) ? capacidad : componente.getCapacidad(),
					StringUtils.hasText(remito) ? remito : componente.getRemito(),
					StringUtils.hasText(ordenCompra) ? ordenCompra : componente.getOrdenCompra(),
					StringUtils.hasText(proveedor) ? proveedor : componente.getProveedor(),
					componente.getUbicacion(),
					StringUtils.hasText(observaciones) ? observaciones : componente.getObservaciones(),
					componente.isActivo());
		}
		stockComponenteRepository.saveAll(componentes);
		auditoriaService.registrar("STOCK", "ACTUALIZAR_LOTE", "StockComponente", null,
				"Datos tecnicos y administrativos actualizados en " + componentes.size() + " componentes de stock.");
		return componentes.size();
	}

	@Transactional
	public StockComponente reservar(Long id) {
		StockComponente componente = stockComponenteRepository.findById(id)
				.orElseThrow(() -> new StockComponenteNoEncontradoException(id));
		if (componente.getEstado() != EstadoStockComponente.DISPONIBLE) {
			throw new StockComponenteNoDisponibleException(id);
		}
		componente.reservar();
		auditoriaService.registrar("STOCK", "RESERVAR", "StockComponente", componente.getId(),
				"Componente de stock reservado: " + componente.getDescripcion() + ".");
		return componente;
	}

	@Transactional
	public StockComponente asignarReservado(Long id) {
		StockComponente componente = stockComponenteRepository.findById(id)
				.orElseThrow(() -> new StockComponenteNoEncontradoException(id));
		if (componente.getEstado() == EstadoStockComponente.ASIGNADO) {
			return componente;
		}
		if (componente.getEstado() != EstadoStockComponente.RESERVADO) {
			throw new StockComponenteNoReservadoException(id);
		}
		componente.asignar();
		auditoriaService.registrar("STOCK", "ASIGNAR", "StockComponente", componente.getId(),
				"Salida real confirmada para stock: " + componente.getDescripcion() + ".");
		return componente;
	}

	@Transactional
	public StockComponente liberar(Long id) {
		StockComponente componente = stockComponenteRepository.findById(id)
				.orElseThrow(() -> new StockComponenteNoEncontradoException(id));
		componente.liberar();
		auditoriaService.registrar("STOCK", "LIBERAR", "StockComponente", componente.getId(),
				"Componente de stock liberado a disponible: " + componente.getDescripcion() + ".");
		return componente;
	}

	@Transactional
	public void eliminar(Long id) {
		StockComponente componente = stockComponenteRepository.findById(id)
				.orElseThrow(() -> new StockComponenteNoEncontradoException(id));
		
		if (componente.getEstado() == EstadoStockComponente.ASIGNADO) {
			throw new IllegalStateException("No se puede eliminar del stock porque ya fue ASIGNADO a un equipo. Para removerlo, ve a la ficha del equipo y desvincúlalo.");
		}
		
		stockComponenteRepository.delete(componente);
		auditoriaService.registrar("STOCK", "ELIMINAR", "StockComponente", id,
				"Componente de stock eliminado: " + componente.getDescripcion() + ".");
	}

	private void aplicarCampos(StockComponente componente, GuardarStockComponenteCommand command) {
		componente.actualizar(
				command.tipo(),
				command.estado(),
				textoRequerido(command.descripcion(), "descripcion"),
				textoOpcional(command.marca()),
				textoOpcional(command.modelo()),
				textoOpcional(command.serial()),
				textoOpcional(command.capacidad()),
				textoOpcional(command.remito()),
				textoOpcional(command.ordenCompra()),
				textoOpcional(command.proveedor()),
				textoOpcional(command.ubicacion()),
				textoOpcional(command.observaciones()),
				command.activo());
	}

	private StockComponenteDetalle toDetalle(StockComponente componente) {
		var vinculo = buscarVinculoActivo(componente);
		return new StockComponenteDetalle(
				componente.getId(),
				componente.getTipo(),
				componente.getEstado(),
				componente.getDescripcion(),
				componente.getMarca(),
				componente.getModelo(),
				componente.getSerial(),
				componente.getCapacidad(),
				componente.getRemito(),
				componente.getOrdenCompra(),
				componente.getProveedor(),
				componente.getIngresadoPor(),
				componente.isDatosCompletos(),
				camposFaltantesParaStock(componente),
				componente.getUbicacion(),
				componente.getObservaciones(),
				vinculo == null ? null : vinculo.getEquipo().getId(),
				vinculo == null ? null : vinculo.getEquipo().getNombre(),
				vinculo == null ? null : vinculo.getEquipo().getUltimoUsuario(),
				componente.isActivo());
	}

	private void sincronizarEstadoConVinculoActivo(StockComponente componente) {
		if (!StringUtils.hasText(componente.getSerial())) {
			return;
		}
		var vinculo = buscarVinculoActivo(componente);
		if (vinculo != null) {
			marcarAsignadoConVinculoActivo(componente, vinculo);
			return;
		}
		liberarAsignadoSinVinculoActivo(componente);
	}

	private void marcarAsignadoConVinculoActivo(StockComponente componente, Componente vinculo) {
		if (componente.getEstado() == EstadoStockComponente.ASIGNADO) {
			return;
		}
		componente.actualizar(
				componente.getTipo(),
				EstadoStockComponente.ASIGNADO,
				componente.getDescripcion(),
				componente.getMarca(),
				componente.getModelo(),
				componente.getSerial(),
				componente.getCapacidad(),
				componente.getRemito(),
				componente.getOrdenCompra(),
				componente.getProveedor(),
				componente.getUbicacion(),
				observacionConNota(componente.getObservaciones(), "Asignado automáticamente: tiene equipo activo vinculado (" + vinculo.getEquipo().getNombre() + ")."),
				componente.isActivo());
		auditoriaService.registrar("STOCK", "ASIGNAR_POR_VINCULO", "StockComponente", componente.getId(),
				"Stock marcado como ASIGNADO porque tiene equipo activo vinculado: " + componente.getDescripcion() + ".");
	}

	private void liberarAsignadoSinVinculoActivo(StockComponente componente) {
		if (componente.getEstado() != EstadoStockComponente.ASIGNADO) {
			return;
		}
		componente.liberar();
		componente.actualizar(
				componente.getTipo(),
				EstadoStockComponente.DISPONIBLE,
				componente.getDescripcion(),
				componente.getMarca(),
				componente.getModelo(),
				componente.getSerial(),
				componente.getCapacidad(),
				componente.getRemito(),
				componente.getOrdenCompra(),
				componente.getProveedor(),
				componente.getUbicacion(),
				observacionConNota(componente.getObservaciones(), "Liberado automáticamente: no tiene equipo activo vinculado."),
				componente.isActivo());
		auditoriaService.registrar("STOCK", "LIBERAR_HUERFANO", "StockComponente", componente.getId(),
				"Stock asignado liberado automáticamente por no tener equipo activo vinculado: " + componente.getDescripcion() + ".");
	}

	private Componente buscarVinculoActivo(StockComponente componente) {
		if (!StringUtils.hasText(componente.getSerial())) {
			return null;
		}
		// Serial + tipo evita mostrar como vinculada una pieza distinta con el mismo codigo mal cargado.
		return componenteRepository.findBySerialAndActivoTrue(componente.getSerial()).stream()
				.filter(c -> c.getTipo() == componente.getTipo())
				.findFirst()
				.orElse(null);
	}

	private String observacionConNota(String observaciones, String nota) {
		return StringUtils.hasText(observaciones) ? observaciones.trim() + " " + nota : nota;
	}

	private List<String> camposFaltantesParaStock(StockComponente componente) {
		List<String> campos = new ArrayList<>();
		if (componente.getTipo() == TipoComponente.PENDIENTE) {
			campos.add("Tipo real");
		}
		if (!tieneDescripcionReal(componente.getDescripcion())) {
			campos.add("Descripcion real");
		}
		return List.copyOf(campos);
	}

	private boolean tieneDescripcionReal(String descripcion) {
		return StringUtils.hasText(descripcion)
				&& !descripcion.toLowerCase(Locale.ROOT).contains("pendiente");
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

	public record GuardarStockComponenteCommand(
			TipoComponente tipo,
			EstadoStockComponente estado,
			String descripcion,
			String marca,
			String modelo,
			String serial,
			String capacidad,
			String remito,
			String ordenCompra,
			String proveedor,
			String ingresadoPor,
			String ubicacion,
			String observaciones,
			boolean activo) {
	}

	public record CrearStockPendienteLoteCommand(
			List<String> codigos,
			String ingresadoPor) {
		public CrearStockPendienteLoteCommand {
			codigos = codigos == null ? List.of() : List.copyOf(codigos);
		}
	}

	public record ActualizarStockLoteCommand(
			List<Long> ids,
			TipoComponente tipo,
			EstadoStockComponente estado,
			String descripcion,
			String marca,
			String modelo,
			String capacidad,
			String remito,
			String ordenCompra,
			String proveedor,
			String observaciones) {
		public ActualizarStockLoteCommand {
			ids = ids == null ? List.of() : List.copyOf(ids);
		}
	}

	public record StockComponenteDetalle(
			Long id,
			TipoComponente tipo,
			EstadoStockComponente estado,
			String descripcion,
			String marca,
			String modelo,
			String serial,
			String capacidad,
			String remito,
			String ordenCompra,
			String proveedor,
			String ingresadoPor,
			boolean datosCompletos,
			List<String> camposFaltantes,
			String ubicacion,
			String observaciones,
			Long equipoId,
			String equipoNombre,
			String ultimoUsuarioEquipo,
			boolean activo) {
	}

	public static class StockComponenteNoEncontradoException extends RuntimeException {

		public StockComponenteNoEncontradoException(Long id) {
			super("Componente de stock no encontrado: " + id);
		}
	}

	public static class StockComponenteNoDisponibleException extends RuntimeException {

		public StockComponenteNoDisponibleException(Long id) {
			super("Componente de stock no disponible: " + id);
		}
	}

	public static class StockComponenteNoReservadoException extends RuntimeException {

		public StockComponenteNoReservadoException(Long id) {
			super("Componente de stock no reservado: " + id);
		}
	}

	public static class LoteStockSinSeleccionException extends RuntimeException {
		public LoteStockSinSeleccionException() {
			super("Debe seleccionar al menos un componente de stock.");
		}
	}

	public static class LoteStockSinDatosException extends RuntimeException {
		public LoteStockSinDatosException() {
			super("Debe informar al menos un dato administrativo para aplicar.");
		}
	}

	public static class LoteStockConComponentesInvalidosException extends RuntimeException {
		public LoteStockConComponentesInvalidosException() {
			super("La seleccion contiene componentes inexistentes.");
		}
	}
}
