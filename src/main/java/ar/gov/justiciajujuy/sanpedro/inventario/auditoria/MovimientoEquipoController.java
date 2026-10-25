package ar.gov.justiciajujuy.sanpedro.inventario.auditoria;

import ar.gov.justiciajujuy.sanpedro.inventario.equipos.Equipo;
import ar.gov.justiciajujuy.sanpedro.inventario.equipos.EquipoRepository;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin/equipos/{equipoId}/auditoria")
public class MovimientoEquipoController {

    private static final String MODULO_AUDITORIA = "AUDITORIA";
    private static final String MODULO_EQUIPOS = "EQUIPOS";
    private static final String PERMISO_VER = "VER";
    private static final String PERMISO_EDITAR = "EDITAR";

    private final AuditoriaService auditoriaService;
    private final EquipoRepository equipoRepository;
    private final AuthorizationService authorizationService;

    public MovimientoEquipoController(
            AuditoriaService auditoriaService,
            EquipoRepository equipoRepository,
            AuthorizationService authorizationService) {
        this.auditoriaService = auditoriaService;
        this.equipoRepository = equipoRepository;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public String verHistorial(
            @PathVariable Long equipoId,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) TipoMovimiento tipo,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        exigirPermisoAuditoriaVer(userDetails);
        Equipo equipo = equipoRepository.findById(equipoId).orElseThrow();
        model.addAttribute("equipo", equipo);
        model.addAttribute("historial", auditoriaService.obtenerHistorialFiltrado(equipoId, usuario, tipo));
        model.addAttribute("usuarioFiltro", usuario);
        model.addAttribute("tipoFiltro", tipo);
        model.addAttribute("tiposMovimiento", TipoMovimiento.values());
        return "admin/equipo-auditoria";
    }

    @PostMapping
    public String registrarMovimiento(@PathVariable Long equipoId,
                                      @RequestParam TipoMovimiento tipoMovimiento,
                                      @RequestParam(required = false) String usuarioDestino,
                                      @RequestParam(required = false) String ubicacionOrigen,
                                      @RequestParam(required = false) String ubicacionDestino,
                                      @RequestParam(required = false) String observaciones,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      Authentication authentication) {
        exigirPermisoEquiposEditar(userDetails);
        String admin = authentication != null ? authentication.getName() : "Sistema";
        auditoriaService.registrarMovimiento(equipoId, tipoMovimiento, usuarioDestino, ubicacionOrigen, ubicacionDestino, admin, observaciones);
        return "redirect:/admin/equipos/" + equipoId + "/auditoria?success=true";
    }

    @GetMapping("/{movId}/pdf")
    public ResponseEntity<byte[]> descargarPdf(
            @PathVariable Long equipoId,
            @PathVariable Long movId,
            @AuthenticationPrincipal UserDetails userDetails) {
        exigirPermisoAuditoriaVer(userDetails);
        try {
            byte[] pdfBytes = auditoriaService.generarActaPdf(movId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "Acta_Movimiento_" + movId + ".pdf");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void exigirPermisoAuditoriaVer(UserDetails userDetails) {
        if (userDetails == null || authorizationService.esReporteDeMaquina(userDetails)
                || !authorizationService.tienePermiso(userDetails, MODULO_AUDITORIA, PERMISO_VER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para ver auditoria.");
        }
    }

    private void exigirPermisoEquiposEditar(UserDetails userDetails) {
        if (userDetails == null || authorizationService.esReporteDeMaquina(userDetails)
                || !authorizationService.tienePermiso(userDetails, MODULO_EQUIPOS, PERMISO_EDITAR)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para editar equipos.");
        }
    }
}
