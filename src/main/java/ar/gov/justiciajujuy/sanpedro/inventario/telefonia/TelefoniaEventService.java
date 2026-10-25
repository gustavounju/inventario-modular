package ar.gov.justiciajujuy.sanpedro.inventario.telefonia;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TelefoniaEventService {

    public static final int MAX_CONEXIONES_SIMULTANEAS = 50;
    public static final long TIMEOUT_CONEXION_MS = 30 * 60 * 1000L; // 30 minutos

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter crearConexion() {
        if (this.emitters.size() >= MAX_CONEXIONES_SIMULTANEAS) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Limite de conexiones SSE simultaneas alcanzado (" + MAX_CONEXIONES_SIMULTANEAS + ").");
        }
        SseEmitter emitter = new SseEmitter(TIMEOUT_CONEXION_MS);
        this.emitters.add(emitter);
        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> this.emitters.remove(emitter));
        emitter.onError(e -> this.emitters.remove(emitter));
        return emitter;
    }

    public int cantidadConexionesActivas() {
        return this.emitters.size();
    }

    public void notificarLlamadaEntrante(String caller, String callerName, String callee, String status) {
        Map<String, String> evento = Map.of(
            "caller", caller != null ? caller : "",
            "callerName", callerName != null ? callerName : "",
            "callee", callee != null ? callee : "",
            "status", status != null ? status : ""
        );
        
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
        this.emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("llamada").data(evento));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        });
        this.emitters.removeAll(deadEmitters);
    }
}
