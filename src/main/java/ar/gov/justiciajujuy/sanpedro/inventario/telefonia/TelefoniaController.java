package ar.gov.justiciajujuy.sanpedro.inventario.telefonia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public TelefoniaController(TelefoniaEventService eventService, UcmApiClient ucmApiClient) {
        this.eventService = eventService;
        this.ucmApiClient = ucmApiClient;
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
        
        String clientIp = request.getRemoteAddr();
        
        // Validación de IP permitida
        if (!"10.15.0.2".equals(clientIp) && !"127.0.0.1".equals(clientIp) && !"0:0:0:0:0:0:0:1".equals(clientIp)) {
            log.warn("Intento de webhook desde IP no autorizada: {}", clientIp);
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

        String estadoDefinitivo = payload.getStatus() != null ? payload.getStatus() : payload.getEvent();
        
        eventService.notificarLlamadaEntrante(
                payload.getCaller(), 
                payload.getCallerName(), 
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
        return ResponseEntity.ok(ucmApiClient.obtenerHistorialCdr());
    }
}
