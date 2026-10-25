package ar.gov.justiciajujuy.sanpedro.inventario.telefonia;

import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.TelefoniaConfigurationService;
import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.TelefoniaRuntimeConfig;
import ar.gov.justiciajujuy.sanpedro.inventario.security.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
@RequestMapping("/api/telefonia")
public class TelefoniaController {

    private static final Logger log = LoggerFactory.getLogger(TelefoniaController.class);
    private final TelefoniaEventService eventService;
    private final UcmApiClient ucmApiClient;
    private final TelefoniaConfigurationService configurationService;
    private final DirectorioTelefonicoService directorioTelefonicoService;
    private final AuthorizationService authorizationService;

    @Value("${ucm.webhook.ips:10.15.0.2,127.0.0.1,0:0:0:0:0:0:0:1}")
    private java.util.List<String> defaultAllowedIps;

    @Value("${ucm.simulacion.enabled:false}")
    private boolean simulacionEnabled;

    @Autowired
    public TelefoniaController(
            TelefoniaEventService eventService,
            UcmApiClient ucmApiClient,
            @Autowired(required = false) TelefoniaConfigurationService configurationService,
            @Autowired(required = false) DirectorioTelefonicoService directorioTelefonicoService,
            @Autowired(required = false) AuthorizationService authorizationService) {
        this.eventService = eventService;
        this.ucmApiClient = ucmApiClient;
        this.configurationService = configurationService;
        this.directorioTelefonicoService = directorioTelefonicoService != null ? directorioTelefonicoService : new DirectorioTelefonicoService();
        this.authorizationService = authorizationService;
    }

    public TelefoniaController(
            TelefoniaEventService eventService,
            UcmApiClient ucmApiClient,
            TelefoniaConfigurationService configurationService) {
        this(eventService, ucmApiClient, configurationService, null, null);
    }

    // 1. Endpoint Receptor de Webhook (UCM6304A)
    @PostMapping(value = "/webhook/llamada", consumes = {"application/json", "application/x-www-form-urlencoded"})
    public ResponseEntity<String> recibirWebhookLlamada(
            @RequestBody(required = false) WebhookLlamadaDto payload,
            @RequestParam(required = false) String caller,
            @RequestParam(required = false) String callerName,
            @RequestParam(required = false) String callee,
            @RequestParam(required = false) String event,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {

        TelefoniaRuntimeConfig config = (configurationService != null)
                ? configurationService.current()
                : null;

        if (config != null && !config.enabled()) {
            log.info("Webhook de telefonía recibido pero la integración está deshabilitada en la configuración.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Telefonía deshabilitada");
        }

        String clientIp = resolveClientIp(request);

        boolean ipPermitida;
        if (config != null) {
            ipPermitida = config.isIpAllowed(clientIp);
        } else {
            ipPermitida = defaultAllowedIps != null && defaultAllowedIps.contains(clientIp);
        }

        if (!ipPermitida) {
            log.warn("Intento de webhook desde IP no autorizada: {} (remoteAddr: {}, X-Forwarded-For: {})",
                    clientIp, request.getRemoteAddr(), request.getHeader("X-Forwarded-For"));
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("IP no autorizada");
        }

        // Si la central envía Form-Data en vez de JSON
        if (payload == null) {
            payload = new WebhookLlamadaDto();
            payload.setCaller(caller);
            payload.setCallerName(callerName);
            payload.setCallee(callee);
            payload.setEvent(event);
            payload.setStatus(status);
        }

        log.info("Llamada en vivo: IP={}, caller={}, callee={}, event={}",
                clientIp, payload.getCaller(), payload.getCallee(), payload.getEvent());

        String callerVal = payload.getCaller();
        String callerNameVal = payload.getCallerName();
        if ((callerNameVal == null || callerNameVal.isBlank() || callerNameVal.equals(callerVal)) && callerVal != null) {
            String resName = directorioTelefonicoService.resolverNombre(callerVal);
            if (!resName.isBlank()) {
                callerNameVal = resName;
                payload.setCallerName(resName);
            }
        }

        String estadoDefinitivo = payload.getStatus() != null ? payload.getStatus() : payload.getEvent();

        eventService.notificarLlamadaEntrante(
                payload.getCaller(),
                callerNameVal,
                payload.getCallee(),
                estadoDefinitivo);

        ucmApiClient.registrarLlamadaEnVivo(
                payload.getCaller(),
                callerNameVal,
                payload.getCallee(),
                estadoDefinitivo);

        return ResponseEntity.ok("OK");
    }

    // 2. Endpoint para suscripción al visor (SSE)
    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter streamLlamadas() {
        return eventService.crearConexion();
    }

    // 3. Endpoint para historial
    @GetMapping(value = "/historial-hoy", produces = "application/json")
    public ResponseEntity<String> obtenerHistorial() {
        return ResponseEntity.ok()
            .header("Cache-Control", "no-store, no-cache, must-revalidate")
            .header("Pragma", "no-cache")
            .body(ucmApiClient.obtenerHistorialCdr());
    }

    // 4. Directorio de internos telefónicos (solo lectura pública para visor)
    @GetMapping(value = "/directorio", produces = "application/json")
    public java.util.List<DirectorioTelefonico> listarDirectorio() {
        return directorioTelefonicoService.listarTodos();
    }

    // 5. Endpoint directo para simular llamadas desde pruebas o desarrollo
    @PostMapping(value = "/simular")
    public ResponseEntity<String> simularLlamada(
            @RequestParam(required = false, defaultValue = "2005") String caller,
            @RequestParam(required = false) String callerName,
            @RequestParam(required = false, defaultValue = "1005") String callee,
            @RequestParam(required = false, defaultValue = "NO ANSWER") String estado,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        if (!simulacionEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Simulación de telefonía deshabilitada en este entorno.");
        }

        if (userDetails == null || authorizationService == null
                || !authorizationService.tienePermiso(userDetails, "SISTEMA", "ADMINISTRAR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Permisos insuficientes: requiere SISTEMA.ADMINISTRAR.");
        }

        String resolvedName = (callerName != null && !callerName.isBlank() && !callerName.equals(caller))
                ? callerName
                : directorioTelefonicoService.resolverNombre(caller);
        if (resolvedName.isBlank()) {
            resolvedName = caller;
        }

        log.info("Simulación de llamada solicitada por {} desde {}: {} ({}) -> {} [{}]",
                userDetails.getUsername(), request.getRemoteAddr(), resolvedName, caller, callee, estado);

        eventService.notificarLlamadaEntrante(caller, resolvedName, callee, estado);
        ucmApiClient.registrarLlamadaEnVivo(caller, resolvedName, callee, estado);

        return ResponseEntity.ok(String.format("Llamada simulada registrada: %s (%s) -> %s [Estado: %s]",
                resolvedName, caller, callee, estado));
    }

    /**
     * Resuelve la IP del cliente siguiendo la misma politica estricta de LanOnlyAccessFilter:
     * Solo confia en X-Forwarded-For si la conexion directa (remoteAddr) proviene de loopback (proxy local).
     */
    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (!isLoopback(remoteAddress)) {
            return remoteAddress;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return remoteAddress;
        }
        return forwardedFor.split(",")[0].trim();
    }

    private boolean isLoopback(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        try {
            return InetAddress.getByName(address.trim()).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
