package ar.gov.justiciajujuy.sanpedro.inventario.telefonia;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TelefoniaEventService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter crearConexion() {
        SseEmitter emitter = new SseEmitter(0L); // Sin timeout
        this.emitters.add(emitter);
        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> this.emitters.remove(emitter));
        emitter.onError(e -> this.emitters.remove(emitter));
        return emitter;
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
