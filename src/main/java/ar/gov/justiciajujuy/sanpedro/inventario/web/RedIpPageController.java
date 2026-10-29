package ar.gov.justiciajujuy.sanpedro.inventario.web;

import java.util.List;
import java.util.Map;

import ar.gov.justiciajujuy.sanpedro.inventario.red.CategoriaIp;
import ar.gov.justiciajujuy.sanpedro.inventario.red.EstadoIp;
import ar.gov.justiciajujuy.sanpedro.inventario.red.RedIp;
import ar.gov.justiciajujuy.sanpedro.inventario.red.RedIpService;
import ar.gov.justiciajujuy.sanpedro.inventario.red.RedIpService.DiscrepanciaIp;
import ar.gov.justiciajujuy.sanpedro.inventario.red.RedIpService.GuardarIpCommand;
import ar.gov.justiciajujuy.sanpedro.inventario.red.RedIpService.MetricasRedIp;
import ar.gov.justiciajujuy.sanpedro.inventario.red.RedIpService.ValidacionIpResult;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService.UsuarioActual;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/red-ips")
public class RedIpPageController {

    private final RedIpService redIpService;
    private final AuthorizationService authorizationService;
    private final String applicationName;
    private final String version;

    public RedIpPageController(
            RedIpService redIpService,
            AuthorizationService authorizationService,
            @Value("${spring.application.name:Inventario Modular}") String applicationName,
            @Value("${inventario.version:1.0.6}") String version) {
        this.redIpService = redIpService;
        this.authorizationService = authorizationService;
        this.applicationName = applicationName;
        this.version = version;
    }

    @GetMapping
    public String index(
            Model model,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado) {

        if (userDetails == null) {
            return "redirect:/login";
        }
        if (!puedeVer(userDetails)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tiene permiso para acceder al módulo de Red e IPs.");
        }

        UsuarioActual usuarioActual = authorizationService.obtenerUsuarioActual(userDetails);
        List<RedIp> ips = (q != null && !q.isBlank())
                ? redIpService.buscar(q)
                : redIpService.listarTodas();

        if (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado)) {
            try {
                EstadoIp estadoFiltro = EstadoIp.valueOf(estado.toUpperCase());
                ips = ips.stream().filter(ip -> ip.getEstado() == estadoFiltro).toList();
            } catch (Exception ignored) {
            }
        }

        MetricasRedIp metricas = redIpService.metricas();
        List<DiscrepanciaIp> discrepancias = redIpService.detectarDiscrepanciasConInventario();

        model.addAttribute("applicationName", applicationName);
        model.addAttribute("version", version);
        model.addAttribute("username", usuarioActual != null ? usuarioActual.username() : "");
        model.addAttribute("displayName", usuarioActual != null ? usuarioActual.nombreVisible() : "");
        model.addAttribute("fuero", usuarioActual != null ? usuarioActual.fuero() : "");
        model.addAttribute("activeModule", "red-ips");
        model.addAttribute("canManageSystem", esAdministrador(userDetails));
        model.addAttribute("puedeModificar", puedeModificar(userDetails));

        model.addAttribute("ips", ips);
        model.addAttribute("metricas", metricas);
        model.addAttribute("discrepancias", discrepancias);
        model.addAttribute("categorias", CategoriaIp.values());
        model.addAttribute("estados", EstadoIp.values());
        model.addAttribute("filtroQuery", q != null ? q : "");
        model.addAttribute("filtroEstado", estado != null ? estado.toUpperCase() : "TODOS");

        return "admin/red-ips";
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam String ip,
            @RequestParam(required = false) String equipo,
            @RequestParam(required = false) String detalle,
            @RequestParam(required = false) String observaciones,
            @RequestParam(required = false) CategoriaIp categoria,
            @RequestParam(required = false) EstadoIp estado,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (!puedeModificar(userDetails)) {
            return "redirect:/admin/red-ips?errorAcceso";
        }

        try {
            String usuario = userDetails != null ? userDetails.getUsername() : "admin";
            GuardarIpCommand cmd = new GuardarIpCommand(
                    ip, equipo, detalle, observaciones, categoria, estado);
            RedIp guardada = redIpService.guardarOAsignar(cmd, usuario);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Dirección IP " + guardada.getIp() + " guardada correctamente (" + guardada.getEstado() + ").");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error al guardar IP: " + e.getMessage());
        }

        return "redirect:/admin/red-ips";
    }

    @PostMapping("/sincronizar-desde-inventario")
    public String sincronizarDesdeInventario(
            @RequestParam String ip,
            @RequestParam String equipo,
            @RequestParam(required = false) String detalle,
            @RequestParam(required = false) String observaciones,
            @RequestParam(required = false, defaultValue = "TERMINAL") CategoriaIp categoria,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        if (!puedeModificar(userDetails)) {
            return "redirect:/admin/red-ips?errorAcceso";
        }
        try {
            String usuario = userDetails != null ? userDetails.getUsername() : "admin";
            GuardarIpCommand cmd = new GuardarIpCommand(
                    ip,
                    equipo,
                    detalle != null && !detalle.isBlank() ? detalle : "Sincronizado desde Script PowerShell",
                    observaciones,
                    categoria,
                    EstadoIp.OCUPADA);
            RedIp guardada = redIpService.guardarOAsignar(cmd, usuario);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "✓ IP " + guardada.getIp() + " sincronizada en el plano con el equipo " + guardada.getEquipo() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error al sincronizar IP: " + e.getMessage());
        }
        return "redirect:/admin/red-ips";
    }

    @PostMapping("/{id}/liberar")
    public String liberar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (!puedeModificar(userDetails)) {
            return "redirect:/admin/red-ips?errorAcceso";
        }

        try {
            String usuario = userDetails != null ? userDetails.getUsername() : "admin";
            RedIp liberada = redIpService.liberar(id, usuario);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "La dirección IP " + liberada.getIp() + " fue liberada exitosamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error al liberar IP: " + e.getMessage());
        }

        return "redirect:/admin/red-ips";
    }

    // --- API auxiliar para validaciones dinámicas en el modal ---

    @GetMapping("/api/validar")
    @ResponseBody
    public ResponseEntity<ValidacionIpResult> validarIp(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String ip,
            @RequestParam(required = false) Long excluirId) {
        exigirAccesoApi(userDetails);
        return ResponseEntity.ok(redIpService.verificarIp(ip, excluirId));
    }

    @GetMapping("/api/siguiente-libre")
    @ResponseBody
    public ResponseEntity<Map<String, String>> siguienteLibre(
            @AuthenticationPrincipal UserDetails userDetails) {
        exigirAccesoApi(userDetails);
        return redIpService.buscarSiguienteLibre()
                .map(ip -> ResponseEntity.ok(Map.of("ip", ip.getIp())))
                .orElseGet(() -> ResponseEntity.ok(Map.of("ip", "")));
    }

    @GetMapping("/api/detalle/{id}")
    @ResponseBody
    public ResponseEntity<?> detalle(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        exigirAccesoApi(userDetails);
        return redIpService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void exigirAccesoApi(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticación requerida.");
        }
        if (!puedeVer(userDetails)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para consultar IPs.");
        }
    }

    private boolean puedeVer(UserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }
        return esAdministrador(userDetails)
                || authorizationService.tienePermiso(userDetails, "EQUIPOS", "VER")
                || authorizationService.tienePermiso(userDetails, "TAREAS", "VER")
                || authorizationService.tienePermiso(userDetails, "TAREAS", "ADMINISTRAR")
                || authorizationService.tienePermiso(userDetails, "EQUIPOS", "ADMINISTRAR");
    }

    private boolean esAdministrador(UserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }
        return authorizationService.tienePermiso(userDetails, "USUARIOS", "ADMINISTRAR")
                || authorizationService.tienePermiso(userDetails, "SISTEMA", "ADMINISTRAR");
    }

    private boolean puedeModificar(UserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }
        return esAdministrador(userDetails)
                || authorizationService.tienePermiso(userDetails, "EQUIPOS", "EDITAR")
                || authorizationService.tienePermiso(userDetails, "EQUIPOS", "ADMINISTRAR")
                || authorizationService.tienePermiso(userDetails, "TAREAS", "EDITAR");
    }
}
