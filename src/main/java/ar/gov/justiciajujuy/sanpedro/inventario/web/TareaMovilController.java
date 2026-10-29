package ar.gov.justiciajujuy.sanpedro.inventario.web;

import ar.gov.justiciajujuy.sanpedro.inventario.componentes.GemeloDigitalService;
import ar.gov.justiciajujuy.sanpedro.inventario.componentes.GemeloDigitalService.ComparacionComponente;
import ar.gov.justiciajujuy.sanpedro.inventario.componentes.TipoComponente;
import ar.gov.justiciajujuy.sanpedro.inventario.equipos.EquipoService;
import ar.gov.justiciajujuy.sanpedro.inventario.equipos.EquipoService.EquipoDetalle;
import ar.gov.justiciajujuy.sanpedro.inventario.equipos.EquipoService.EquipoNoEncontradoException;
import ar.gov.justiciajujuy.sanpedro.inventario.equipos.EquipoService.EquipoResumen;
import ar.gov.justiciajujuy.sanpedro.inventario.movil.ApkDistributionService;
import ar.gov.justiciajujuy.sanpedro.inventario.movil.ApkDistributionService.ApkInfo;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryDomainService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryDomainService.DominioUsuarios;
import ar.gov.justiciajujuy.sanpedro.inventario.security.ActiveDirectoryDomainService.UsuarioDominio;
import ar.gov.justiciajujuy.sanpedro.inventario.security.UsuarioManagementService;
import ar.gov.justiciajujuy.sanpedro.inventario.security.UsuarioManagementService.UsuarioResumen;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaAvisoService;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.TareaTecnicaDetalle;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import ar.gov.justiciajujuy.sanpedro.inventario.movil.ApkPresenciaService;
import ar.gov.justiciajujuy.sanpedro.inventario.movil.ApkPresenciaService.UsuarioConectadoDto;

@Controller
public class TareaMovilController {
    private final AuthorizationService authorization;
    private final ActiveDirectoryDomainService activeDirectoryDomainService;
    private final UsuarioManagementService usuarioManagementService;
    private final TareaAvisoService avisos;
    private final ApkDistributionService apkDistributionService;
    private final ApkPresenciaService apkPresenciaService;
    private final EquipoService equipoService;
    private final GemeloDigitalService gemeloDigitalService;
    private final TareaTecnicaService tareaTecnicaService;

    public TareaMovilController(AuthorizationService authorization,
            ActiveDirectoryDomainService activeDirectoryDomainService,
            UsuarioManagementService usuarioManagementService,
            TareaAvisoService avisos,
            ApkDistributionService apkDistributionService,
            ApkPresenciaService apkPresenciaService,
            EquipoService equipoService,
            GemeloDigitalService gemeloDigitalService,
            TareaTecnicaService tareaTecnicaService) {
        this.authorization = authorization;
        this.activeDirectoryDomainService = activeDirectoryDomainService;
        this.usuarioManagementService = usuarioManagementService;
        this.avisos = avisos;
        this.apkDistributionService = apkDistributionService;
        this.apkPresenciaService = apkPresenciaService;
        this.equipoService = equipoService;
        this.gemeloDigitalService = gemeloDigitalService;
        this.tareaTecnicaService = tareaTecnicaService;
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
        apkPresenciaService.registrarActividad(user, userAgent);
        boolean appInstalada = userAgent != null && userAgent.contains("InventarioLAN/1");
        model.addAttribute("appInstalada", appInstalada);
        model.addAttribute("apkDisponible", apkDistributionService.isAvailable() && !appInstalada);
        model.addAttribute("apkInfo", apkDistributionService.info());
        model.addAttribute("puedeVerStock", authorization.tienePermiso(user, "STOCK", "VER"));
        model.addAttribute("puedeVerEquipos", tienePermisoEquiposMovil(user));
        return "movil/tareas";
    }

    @GetMapping("/movil/stock")
    public String stock(@AuthenticationPrincipal UserDetails user,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            Model model) {
        exigirPermisoStock(user, "VER");
        apkPresenciaService.registrarActividad(user, userAgent);
        boolean appInstalada = userAgent != null && userAgent.contains("InventarioLAN/1");
        model.addAttribute("appInstalada", appInstalada);
        model.addAttribute("apkDisponible", apkDistributionService.isAvailable() && !appInstalada);
        model.addAttribute("apkInfo", apkDistributionService.info());
        model.addAttribute("tiposComponente", TipoComponente.values());
        model.addAttribute("puedeVerEquipos", tienePermisoEquiposMovil(user));
        return "movil/stock";
    }

    @GetMapping("/movil/equipos")
    public String equipos(@AuthenticationPrincipal UserDetails user,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            Model model) {
        exigirPermisoEquiposMovil(user);
        apkPresenciaService.registrarActividad(user, userAgent);
        boolean appInstalada = userAgent != null && userAgent.contains("InventarioLAN/1");
        model.addAttribute("appInstalada", appInstalada);
        model.addAttribute("apkDisponible", apkDistributionService.isAvailable() && !appInstalada);
        model.addAttribute("apkInfo", apkDistributionService.info());
        model.addAttribute("puedeVerStock", authorization.tienePermiso(user, "STOCK", "VER"));
        return "movil/equipos";
    }

    @GetMapping("/api/v1/movil/equipos")
    @ResponseBody
    public List<EquipoResumen> buscarEquiposMovil(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(name = "q", required = false) String query,
            HttpServletResponse response) {
        exigirPermisoEquiposMovil(user);
        response.setHeader("Cache-Control", "no-store");
        return equipoService.listar(query, 0, 40).equipos();
    }

    @GetMapping("/api/v1/movil/equipos/{id}")
    @ResponseBody
    public FichaEquipoMovil detalleEquipoMovil(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            HttpServletResponse response) {
        exigirPermisoEquiposMovil(user);
        response.setHeader("Cache-Control", "no-store");
        try {
            EquipoDetalle equipo = equipoService.obtener(id);
            List<ComparacionComponente> gemelo = gemeloDigitalService.compararEquipo(id);
            List<TareaTecnicaDetalle> tareasEquipo = tareaTecnicaService.buscar(null, id, null);
            return new FichaEquipoMovil(equipo, gemelo, tareasEquipo);
        } catch (EquipoNoEncontradoException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
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
    public SesionMovil sesion(@AuthenticationPrincipal UserDetails user,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletResponse response) {
        exigirPermiso(user);
        apkPresenciaService.registrarActividad(user, userAgent);
        response.setHeader("Cache-Control", "no-store");
        boolean puedeGestionarTareas = authorization.tienePermiso(user, "TAREAS", "EDITAR");
        boolean puedeCrearTareas = puedeGestionarTareas || authorization.tienePermiso(user, "TAREAS", "CREAR");
        boolean puedeOperarPropias = authorization.tienePermiso(user, "TAREAS", "CREAR")
                && !puedeGestionarTareas
                && !authorization.puedeAdministrarUsuarios(user);
        return new SesionMovil(authorization.obtenerUsuarioActual(user),
                puedeCrearTareas,
                puedeGestionarTareas,
                puedeOperarPropias,
                false,
                !puedeOperarPropias,
                !puedeOperarPropias,
                authorization.puedeAdministrarUsuarios(user),
                authorization.tienePermiso(user, "STOCK", "VER"),
                authorization.tienePermiso(user, "STOCK", "EDITAR"));
    }

    @GetMapping("/api/v1/movil/stock/sesion")
    @ResponseBody
    public SesionStockMovil sesionStock(@AuthenticationPrincipal UserDetails user,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletResponse response) {
        exigirPermisoStock(user, "VER");
        apkPresenciaService.registrarActividad(user, userAgent);
        response.setHeader("Cache-Control", "no-store");
        return new SesionStockMovil(authorization.obtenerUsuarioActual(user),
                authorization.tienePermiso(user, "STOCK", "EDITAR"));
    }

    @GetMapping("/api/v1/movil/avisos")
    @ResponseBody
    public TareaAvisoService.LoteAvisos avisos(@AuthenticationPrincipal UserDetails user,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestParam(required = false) Long despuesDe, HttpServletResponse response) {
        exigirPermiso(user);
        apkPresenciaService.registrarActividad(user, userAgent);
        if (despuesDe != null && despuesDe < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor invalido.");
        }
        response.setHeader("Cache-Control", "no-store");
        boolean recibeAvisosGenerales = authorization.tienePermiso(user, "TAREAS", "EDITAR")
                || authorization.puedeAdministrarUsuarios(user);
        return avisos.consultar(despuesDe, user.getUsername(), recibeAvisosGenerales);
    }

    @GetMapping("/api/v1/movil/tecnicos-conectados")
    @ResponseBody
    public List<UsuarioConectadoDto> tecnicosConectados() {
        return apkPresenciaService.obtenerConectados();
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

    @GetMapping("/api/v1/movil/tecnicos-asignables")
    @ResponseBody
    public TecnicosAsignables tecnicosAsignables(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(name = "q", required = false) String query,
            HttpServletResponse response) {
        exigirPermiso(user);
        response.setHeader("Cache-Control", "no-store");
        String queryNormalizada = query == null ? "" : query.trim();
        Map<String, TecnicoAsignable> usuarios = new LinkedHashMap<>();
        // La asignacion opcional mezcla tecnicos locales con AD; si AD cae, la lista local sigue operativa.
        for (UsuarioResumen tecnico : usuarioManagementService.listarTecnicosAsignables()) {
            TecnicoAsignable opcion = new TecnicoAsignable(
                    tecnico.username(),
                    tecnico.nombreVisible(),
                    tecnico.fuero(),
                    tecnico.origen(),
                    "LOCAL");
            if (coincide(opcion, queryNormalizada)) {
                usuarios.put(opcion.username().toLowerCase(Locale.ROOT), opcion);
            }
        }

        DominioUsuarios dominio = activeDirectoryDomainService.buscarUsuarios(queryNormalizada);
        for (UsuarioDominio usuarioDominio : dominio.usuarios()) {
            TecnicoAsignable opcion = new TecnicoAsignable(
                    usuarioDominio.username(),
                    usuarioDominio.nombreVisible(),
                    usuarioDominio.fuero(),
                    "AD",
                    "AD");
            usuarios.putIfAbsent(opcion.username().toLowerCase(Locale.ROOT), opcion);
        }
        List<TecnicoAsignable> lista = new ArrayList<>(usuarios.values());
        return new TecnicosAsignables(dominio.disponible(), dominio.consultaRealizada(), queryNormalizada,
                dominio.mensaje(), lista);
    }

    private void exigirPermiso(UserDetails user) {
        if (!authorization.tienePermiso(user, "TAREAS", "VER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para ver tareas.");
        }
    }

    private boolean tienePermisoEquiposMovil(UserDetails user) {
        return authorization.tienePermiso(user, "EQUIPOS", "VER")
                || authorization.tienePermiso(user, "TAREAS", "VER")
                || authorization.tienePermiso(user, "STOCK", "VER");
    }

    private void exigirPermisoEquiposMovil(UserDetails user) {
        if (!tienePermisoEquiposMovil(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para consultar equipos en terreno.");
        }
    }

    private void exigirPermisoStock(UserDetails user, String permiso) {
        if (!authorization.tienePermiso(user, "STOCK", permiso)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para operar stock.");
        }
    }

    private void exigirUsuarioAutorizado(UserDetails user) {
        if (!authorization.obtenerUsuarioActual(user).autorizado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no autorizado para descargar la APK.");
        }
    }

    private boolean coincide(TecnicoAsignable usuario, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String aguja = query.toLowerCase(Locale.ROOT);
        return contiene(usuario.username(), aguja)
                || contiene(usuario.nombreVisible(), aguja)
                || contiene(usuario.fuero(), aguja);
    }

    private boolean contiene(String valor, String aguja) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(aguja);
    }

    public record FichaEquipoMovil(
            EquipoDetalle equipo,
            List<ComparacionComponente> gemelo,
            List<TareaTecnicaDetalle> tareas) { }

    public record SesionMovil(AuthorizationService.UsuarioActual usuario, boolean puedeCrear, boolean puedeEditar,
            boolean puedeOperarPropias, boolean puedeAsignarResponsable, boolean mostrarMisTareas,
            boolean mostrarFinalizadas, boolean administrador, boolean puedeVerStock, boolean puedeEditarStock) { }

    public record SesionStockMovil(AuthorizationService.UsuarioActual usuario, boolean puedeEditarStock) { }

    public record TecnicoAsignable(
            String username,
            String nombreVisible,
            String fuero,
            String origen,
            String fuente) { }

    public record TecnicosAsignables(
            boolean adDisponible,
            boolean consultaAdRealizada,
            String query,
            String mensaje,
            List<TecnicoAsignable> usuarios) { }
}
