package ar.gov.justiciajujuy.sanpedro.inventario.web;

import ar.gov.justiciajujuy.sanpedro.inventario.movil.ApkDistributionService;
import ar.gov.justiciajujuy.sanpedro.inventario.movil.ApkDistributionService.ApkInfo;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryDomainService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryDomainService.DominioUsuarios;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaAvisoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.core.io.Resource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class TareaMovilController {
    private final AuthorizationService authorization;
    private final ActiveDirectoryDomainService activeDirectoryDomainService;
    private final TareaAvisoService avisos;
    private final ApkDistributionService apkDistributionService;

    public TareaMovilController(AuthorizationService authorization,
            ActiveDirectoryDomainService activeDirectoryDomainService,
            TareaAvisoService avisos,
            ApkDistributionService apkDistributionService) {
        this.authorization = authorization;
        this.activeDirectoryDomainService = activeDirectoryDomainService;
        this.avisos = avisos;
        this.apkDistributionService = apkDistributionService;
    }

    @GetMapping("/movil/login")
    public String login() {
        return "movil/login";
    }

    @GetMapping({"/movil", "/movil/tareas"})
    public String tareas(@AuthenticationPrincipal UserDetails user,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            Model model) {
        exigirPermiso(user);
        boolean appInstalada = userAgent != null && userAgent.contains("InventarioLAN/1");
        model.addAttribute("appInstalada", appInstalada);
        model.addAttribute("apkDisponible", apkDistributionService.isAvailable() && !appInstalada);
        model.addAttribute("apkInfo", apkDistributionService.info());
        return "movil/tareas";
    }

    @GetMapping("/api/v1/movil/apk")
    public ResponseEntity<Resource> descargarApk(@AuthenticationPrincipal UserDetails user) {
        exigirUsuarioAutorizado(user);
        Resource apk = apkDistributionService.resource();
        if (!apkDistributionService.isAvailable()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "APK no disponible.");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(apk.getFilename()).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                .body(apk);
    }

    @GetMapping("/api/v1/movil/apk/info")
    @ResponseBody
    public ApkInfo apkInfo(@AuthenticationPrincipal UserDetails user, HttpServletResponse response) {
        exigirUsuarioAutorizado(user);
        response.setHeader("Cache-Control", "no-store");
        return apkDistributionService.info();
    }

    @GetMapping("/api/v1/movil/sesion")
    @ResponseBody
    public SesionMovil sesion(@AuthenticationPrincipal UserDetails user, HttpServletResponse response) {
        exigirPermiso(user);
        response.setHeader("Cache-Control", "no-store");
        return new SesionMovil(authorization.obtenerUsuarioActual(user),
                authorization.tienePermiso(user, "TAREAS", "EDITAR"),
                authorization.puedeAdministrarUsuarios(user));
    }

    @GetMapping("/api/v1/movil/avisos")
    @ResponseBody
    public TareaAvisoService.LoteAvisos avisos(@AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) Long despuesDe, HttpServletResponse response) {
        exigirPermiso(user);
        if (despuesDe != null && despuesDe < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor invalido.");
        }
        response.setHeader("Cache-Control", "no-store");
        return avisos.consultar(despuesDe, user.getUsername());
    }

    @GetMapping("/api/v1/movil/usuarios-dominio")
    @ResponseBody
    public DominioUsuarios usuariosDominio(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(name = "q", required = false) String query,
            HttpServletResponse response) {
        exigirPermiso(user);
        // El autocompletado se consulta desde celulares compartidos en LAN; evitamos cachear nombres de AD.
        response.setHeader("Cache-Control", "no-store");
        return activeDirectoryDomainService.buscarUsuarios(query);
    }

    private void exigirPermiso(UserDetails user) {
        if (!authorization.tienePermiso(user, "TAREAS", "VER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para ver tareas.");
        }
    }

    private void exigirUsuarioAutorizado(UserDetails user) {
        if (!authorization.obtenerUsuarioActual(user).autorizado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no autorizado para descargar la APK.");
        }
    }

    public record SesionMovil(AuthorizationService.UsuarioActual usuario, boolean puedeEditar, boolean administrador) { }
}
