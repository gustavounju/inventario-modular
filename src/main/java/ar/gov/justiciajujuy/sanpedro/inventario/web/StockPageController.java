package ar.gov.justiciajujuy.sanpedro.inventario.web;

import ar.gov.justiciajujuy.sanpedro.inventario.componentes.TipoComponente;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import ar.gov.justiciajujuy.sanpedro.inventario.stock.EstadoStockComponente;
import ar.gov.justiciajujuy.sanpedro.inventario.stock.StockService;
import ar.gov.justiciajujuy.sanpedro.inventario.stock.StockService.ActualizarStockLoteCommand;
import ar.gov.justiciajujuy.sanpedro.inventario.stock.StockService.GuardarStockComponenteCommand;
import ar.gov.justiciajujuy.sanpedro.inventario.stock.StockService.StockComponenteDetalle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StockPageController {

	private static final String MODULO_STOCK = "STOCK";
	private static final String PERMISO_VER = "VER";
	private static final String PERMISO_EDITAR = "EDITAR";

	private final AuthorizationService authorizationService;
	private final StockService stockService;

	public StockPageController(AuthorizationService authorizationService, StockService stockService) {
		this.authorizationService = authorizationService;
		this.stockService = stockService;
	}

	@GetMapping("/admin/stock")
	public String stock(
			Model model,
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam(required = false) String creado) {
		exigirPermiso(userDetails, PERMISO_VER);
		prepararModelo(model, userDetails, new StockForm());
		model.addAttribute("creado", "1".equals(creado));
		return "admin/stock";
	}

	@PostMapping("/admin/stock/componentes")
	public String crear(
			Model model,
			@AuthenticationPrincipal UserDetails userDetails,
			@Valid @ModelAttribute("stockForm") StockForm stockForm,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		exigirPermiso(userDetails, PERMISO_EDITAR);
		if (bindingResult.hasErrors()) {
			prepararModelo(model, userDetails, stockForm);
			return "admin/stock";
		}
		stockService.crear(stockForm.toCommand(userDetails.getUsername()));
		redirectAttributes.addAttribute("creado", "1");
		return "redirect:/admin/stock";
	}

	@PostMapping("/admin/stock/componentes/{id}")
	public String actualizar(
			Model model,
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@Valid @ModelAttribute("stockForm") StockForm stockForm,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		exigirPermiso(userDetails, PERMISO_EDITAR);
		if (bindingResult.hasErrors()) {
			prepararModelo(model, userDetails, stockForm);
			return "admin/stock";
		}
		stockService.actualizar(id, stockForm.toCommand(null));
		redirectAttributes.addAttribute("creado", "1");
		return "redirect:/admin/stock";
	}

	@PostMapping({"/admin/stock/componentes/{id}/eliminar", "/admin/stock/{id}/eliminar"})
	public String eliminar(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			RedirectAttributes redirectAttributes) {
		exigirPermiso(userDetails, PERMISO_EDITAR);
		stockService.eliminar(id);
		redirectAttributes.addFlashAttribute("eliminado", true);
		return "redirect:/admin/stock";
	}

	@PostMapping("/admin/stock/componentes/lote")
	public String actualizarLote(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam(required = false) java.util.List<Long> componentesIds,
			@RequestParam(required = false) String tipo,
			@RequestParam(required = false) String estado,
			@RequestParam(required = false) String descripcion,
			@RequestParam(required = false) String marca,
			@RequestParam(required = false) String modelo,
			@RequestParam(required = false) String capacidad,
			@RequestParam(required = false) String remito,
			@RequestParam(required = false) String ordenCompra,
			@RequestParam(required = false) String proveedor,
			@RequestParam(required = false) String observaciones,
			RedirectAttributes redirectAttributes) {
		exigirPermiso(userDetails, PERMISO_EDITAR);
		try {
			int actualizados = stockService.actualizarDatosAdministrativosEnLote(
					new ActualizarStockLoteCommand(componentesIds, parseTipo(tipo), parseEstado(estado), descripcion,
							marca, modelo, capacidad, remito, ordenCompra, proveedor, observaciones));
			redirectAttributes.addAttribute("loteActualizado", actualizados);
		} catch (StockService.LoteStockSinSeleccionException exception) {
			redirectAttributes.addAttribute("errorLote", "seleccion");
		} catch (StockService.LoteStockSinDatosException exception) {
			redirectAttributes.addAttribute("errorLote", "datos");
		} catch (StockService.LoteStockConComponentesInvalidosException exception) {
			redirectAttributes.addAttribute("errorLote", "componentes");
		}
		return "redirect:/admin/stock#tab-disponibles";
	}

	private void prepararModelo(Model model, UserDetails userDetails, StockForm stockForm) {
		var componentes = stockService.listarDisponiblesYActivos();
		var componentesPendientes = componentes.stream().filter(c -> !c.datosCompletos()).toList();
		var componentesStock = componentes.stream().filter(StockComponenteDetalle::datosCompletos).toList();
		long disponiblesCount = componentesStock.stream().filter(c -> c.estado() == EstadoStockComponente.DISPONIBLE).count();
		long reservadosCount = componentesStock.stream().filter(c -> c.estado() == EstadoStockComponente.RESERVADO).count();
		long asignadosCount = componentesStock.stream().filter(c -> c.estado() == EstadoStockComponente.ASIGNADO).count();
		long pendientesCount = componentesPendientes.size();

		model.addAttribute("componentesPendientes", componentesPendientes);
		model.addAttribute("componentesStock", componentesStock);
		model.addAttribute("totalStock", componentesStock.size());
		model.addAttribute("totalComponentes", componentes.size());
		model.addAttribute("disponiblesCount", disponiblesCount);
		model.addAttribute("reservadosCount", reservadosCount);
		model.addAttribute("asignadosCount", asignadosCount);
		model.addAttribute("pendientesCount", pendientesCount);
		model.addAttribute("stockForm", stockForm);
		model.addAttribute("tiposComponente", TipoComponente.values());
		model.addAttribute("estadosStock", EstadoStockComponente.values());
		model.addAttribute("puedeEditarStock", authorizationService.tienePermiso(userDetails, MODULO_STOCK, PERMISO_EDITAR));
		model.addAttribute("puedeVerOrdenes", authorizationService.tienePermiso(userDetails, "ORDENES_ARMADO", PERMISO_VER));
		model.addAttribute("puedeVerEquipos", authorizationService.tienePermiso(userDetails, "EQUIPOS", PERMISO_VER));
		model.addAttribute("puedeVerDiferencias", authorizationService.tienePermiso(userDetails, "COMPONENTES", PERMISO_VER));
	}

	private void exigirPermiso(UserDetails userDetails, String permiso) {
		if (!authorizationService.tienePermiso(userDetails, MODULO_STOCK, permiso)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para operar stock.");
		}
	}

	private TipoComponente parseTipo(String tipo) {
		return StringUtils.hasText(tipo) ? TipoComponente.valueOf(tipo) : null;
	}

	private EstadoStockComponente parseEstado(String estado) {
		return StringUtils.hasText(estado) ? EstadoStockComponente.valueOf(estado) : null;
	}

	public static class StockForm {

		@NotNull
		private TipoComponente tipo = TipoComponente.RAM;

		@NotNull
		private EstadoStockComponente estado = EstadoStockComponente.DISPONIBLE;

		@NotBlank
		@Size(max = 255)
		private String descripcion;

		@Size(max = 120)
		private String marca;

		@Size(max = 180)
		private String modelo;

		@Size(max = 180)
		private String serial;

		@Size(max = 120)
		private String capacidad;

		@Size(max = 120)
		private String ubicacion;

		@Size(max = 500)
		private String observaciones;

		private boolean activo = true;

		@Size(max = 80)
		private String remito;

		@Size(max = 80)
		private String ordenCompra;

		@Size(max = 150)
		private String proveedor;

		GuardarStockComponenteCommand toCommand(String ingresadoPor) {
			return new GuardarStockComponenteCommand(tipo, estado, descripcion, marca, modelo, serial, capacidad, remito, ordenCompra, proveedor, ingresadoPor, ubicacion, observaciones, activo);
		}

		public TipoComponente getTipo() { return tipo; }
		public void setTipo(TipoComponente tipo) { this.tipo = tipo; }
		public EstadoStockComponente getEstado() { return estado; }
		public void setEstado(EstadoStockComponente estado) { this.estado = estado; }
		public String getDescripcion() { return descripcion; }
		public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
		public String getMarca() { return marca; }
		public void setMarca(String marca) { this.marca = marca; }
		public String getModelo() { return modelo; }
		public void setModelo(String modelo) { this.modelo = modelo; }
		public String getSerial() { return serial; }
		public void setSerial(String serial) { this.serial = serial; }
		public String getCapacidad() { return capacidad; }
		public void setCapacidad(String capacidad) { this.capacidad = capacidad; }
		public String getUbicacion() { return ubicacion; }
		public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
		public String getObservaciones() { return observaciones; }
		public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
		public boolean isActivo() { return activo; }
		public void setActivo(boolean activo) { this.activo = activo; }
		public String getRemito() { return remito; }
		public void setRemito(String remito) { this.remito = remito; }
		public String getOrdenCompra() { return ordenCompra; }
		public void setOrdenCompra(String ordenCompra) { this.ordenCompra = ordenCompra; }
		public String getProveedor() { return proveedor; }
		public void setProveedor(String proveedor) { this.proveedor = proveedor; }
	}
}
