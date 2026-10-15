package ar.gov.justiciajujuy.sanpedro.inventario.telefonia;

import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.TelefoniaConfigurationService;
import ar.gov.justiciajujuy.sanpedro.inventario.configuracion.TelefoniaRuntimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/telefonia")
public class TelefoniaController {

    private static final Logger log = LoggerFactory.getLogger(TelefoniaController.class);
    private final TelefoniaEventService eventService;
    private final UcmApiClient ucmApiClient;
    private final TelefoniaConfigurationService configurationService;
    private final DirectorioTelefonicoService directorioTelefonicoService;

    @org.springframework.beans.factory.annotation.Value("${ucm.webhook.ips:10.15.0.2,127.0.0.1,0:0:0:0:0:0:0:1}")
    private java.util.List<String> defaultAllowedIps;

    @Autowired
    public TelefoniaController(
            TelefoniaEventService eventService,
            UcmApiClient ucmApiClient,
            @Autowired(required = false) TelefoniaConfigurationService configurationService,
            @Autowired(required = false) DirectorioTelefonicoService directorioTelefonicoService) {
        this.eventService = eventService;
        this.ucmApiClient = ucmApiClient;
        this.configurationService = configurationService;
        this.directorioTelefonicoService = directorioTelefonicoService != null ? directorioTelefonicoService : new DirectorioTelefonicoService();
    }

    public TelefoniaController(
            TelefoniaEventService eventService,
            UcmApiClient ucmApiClient,
            TelefoniaConfigurationService configurationService) {
        this(eventService, ucmApiClient, configurationService, null);
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

        String remoteIp = request.getRemoteAddr();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String proxyIp = (forwardedFor != null && !forwardedFor.isBlank())
                ? (forwardedFor.contains(",") ? forwardedFor.split(",")[0].trim() : forwardedFor.trim())
                : null;

        boolean ipPermitida;
        if (config != null) {
            ipPermitida = config.isIpAllowed(remoteIp) || (proxyIp != null && config.isIpAllowed(proxyIp));
        } else {
            ipPermitida = defaultAllowedIps != null && (defaultAllowedIps.contains(remoteIp) || (proxyIp != null && defaultAllowedIps.contains(proxyIp)));
        }

        if (!ipPermitida) {
            log.warn("Intento de webhook desde IP no autorizada: {} (X-Forwarded-For: {})", remoteIp, proxyIp);
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
                remoteIp, payload.getCaller(), payload.getCallee(), payload.getEvent());

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

    // 4. Directorio de internos telefónicos
    @GetMapping(value = "/directorio", produces = "application/json")
    public java.util.List<DirectorioTelefonico> listarDirectorio() {
        return directorioTelefonicoService.listarTodos();
    }

    @PostMapping(value = "/directorio")
    public ResponseEntity<?> guardarDirectorio(
            @RequestParam String numero,
            @RequestParam String nombre,
            @RequestParam(required = false) String departamento) {
        try {
            DirectorioTelefonico guardado = directorioTelefonicoService.guardarOActualizar(numero, nombre, departamento, "MANUAL");
            return ResponseEntity.ok(guardado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping(value = "/directorio/{id}")
    public ResponseEntity<Void> eliminarDirectorio(@PathVariable Long id) {
        directorioTelefonicoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // 5. Endpoint directo para simular llamadas desde PuTTY o pruebas
    @PostMapping(value = "/simular")
    public ResponseEntity<String> simularLlamada(
            @RequestParam(required = false, defaultValue = "2005") String caller,
            @RequestParam(required = false) String callerName,
            @RequestParam(required = false, defaultValue = "1005") String callee,
            @RequestParam(required = false, defaultValue = "NO ANSWER") String estado,
            HttpServletRequest request) {

        String resolvedName = (callerName != null && !callerName.isBlank() && !callerName.equals(caller))
                ? callerName
                : directorioTelefonicoService.resolverNombre(caller);
        if (resolvedName.isBlank()) {
            resolvedName = caller;
        }

        log.info("Simulación de llamada solicitada desde {}: {} ({}) -> {} [{}]",
                request.getRemoteAddr(), resolvedName, caller, callee, estado);

        eventService.notificarLlamadaEntrante(caller, resolvedName, callee, estado);
        ucmApiClient.registrarLlamadaEnVivo(caller, resolvedName, callee, estado);

        return ResponseEntity.ok(String.format("Llamada simulada registrada: %s (%s) -> %s [Estado: %s]",
                resolvedName, caller, callee, estado));
    }
}
