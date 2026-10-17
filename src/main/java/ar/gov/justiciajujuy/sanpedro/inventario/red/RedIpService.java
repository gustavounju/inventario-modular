package ar.gov.justiciajujuy.sanpedro.inventario.red;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RedIpService {

    private static final Logger log = LoggerFactory.getLogger(RedIpService.class);

    private final RedIpRepository repository;

    public RedIpService(RedIpRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<RedIp> listarTodas() {
        return repository.findAllByOrderByIpNumeroAsc();
    }

    @Transactional(readOnly = true)
    public Optional<RedIp> buscarPorId(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<RedIp> buscarPorIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return Optional.empty();
        }
        return repository.findByIp(ip.trim());
    }

    @Transactional(readOnly = true)
    public Optional<RedIp> buscarSiguienteLibre() {
        return repository.findFirstByEstadoOrderByIpNumeroAsc(EstadoIp.LIBRE);
    }

    @Transactional(readOnly = true)
    public List<RedIp> buscar(String query) {
        if (query == null || query.isBlank()) {
            return listarTodas();
        }
        return repository.buscarPorTexto(query.trim());
    }

    @Transactional(readOnly = true)
    public ValidacionIpResult verificarIp(String ip, Long excluirId) {
        if (ip == null || ip.isBlank()) {
            return new ValidacionIpResult(false, null, null, null, null);
        }
        Optional<RedIp> existente = repository.findByIp(ip.trim());
        if (existente.isEmpty()) {
            return new ValidacionIpResult(false, null, null, null, null);
        }
        RedIp r = existente.get();
        if (excluirId != null && r.getId().equals(excluirId)) {
            return new ValidacionIpResult(false, null, null, null, null);
        }
        boolean estaOcupada = r.getEstado() == EstadoIp.OCUPADA || r.getEstado() == EstadoIp.RESERVADA;
        return new ValidacionIpResult(estaOcupada, r.getEquipo(), r.getDetalle(), r.getObservaciones(), r.getEstado());
    }

    @Transactional
    public RedIp guardarOAsignar(GuardarIpCommand cmd, String usuarioActual) {
        if (cmd.ip() == null || cmd.ip().isBlank()) {
            throw new IllegalArgumentException("Debe ingresar una dirección IP válida (ej: 10.15.0.25).");
        }
        String ipNormalizada = cmd.ip().trim();
        Integer numero = extraerNumeroIp(ipNormalizada);

        RedIp redIp = repository.findByIp(ipNormalizada)
                .orElseGet(() -> {
                    RedIp nueva = new RedIp();
                    nueva.setIp(ipNormalizada);
                    nueva.setIpNumero(numero);
                    nueva.setSubred("10.15.0.0/24");
                    return nueva;
                });

        redIp.setEquipo(cmd.equipo() != null && !cmd.equipo().isBlank() ? cmd.equipo().trim() : null);
        redIp.setDetalle(cmd.detalle() != null && !cmd.detalle().isBlank() ? cmd.detalle().trim() : null);
        redIp.setObservaciones(cmd.observaciones() != null && !cmd.observaciones().isBlank() ? cmd.observaciones().trim() : null);
        redIp.setCategoria(cmd.categoria() != null ? cmd.categoria() : CategoriaIp.OTRO);
        redIp.setEstado(cmd.estado() != null ? cmd.estado() : (redIp.getEquipo() != null ? EstadoIp.OCUPADA : EstadoIp.LIBRE));
        redIp.setActualizadoPor(usuarioActual);

        log.info("IP {} actualizada por {}: Equipo={}, Detalle={}, Estado={}",
                redIp.getIp(), usuarioActual, redIp.getEquipo(), redIp.getDetalle(), redIp.getEstado());

        return repository.save(redIp);
    }

    @Transactional
    public RedIp liberar(Long id, String usuarioActual) {
        RedIp redIp = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la dirección IP con ID: " + id));

        log.info("IP {} liberada por {}. Antes asignada a: {}", redIp.getIp(), usuarioActual, redIp.getEquipo());

        redIp.setEquipo(null);
        redIp.setDetalle(null);
        redIp.setObservaciones(null);
        redIp.setCategoria(CategoriaIp.OTRO);
        redIp.setEstado(EstadoIp.LIBRE);
        redIp.setActualizadoPor(usuarioActual);

        return repository.save(redIp);
    }

    @Transactional(readOnly = true)
    public MetricasRedIp metricas() {
        long total = repository.count();
        long ocupadas = repository.countByEstado(EstadoIp.OCUPADA);
        long reservadas = repository.countByEstado(EstadoIp.RESERVADA);
        long libres = repository.countByEstado(EstadoIp.LIBRE);
        int porcentaje = total > 0 ? (int) Math.round(((double) (ocupadas + reservadas) / total) * 100) : 0;
        return new MetricasRedIp(total, ocupadas, libres, reservadas, porcentaje);
    }

    private Integer extraerNumeroIp(String ip) {
        try {
            String[] partes = ip.split("\\.");
            if (partes.length == 4) {
                return Integer.parseInt(partes[3].trim());
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    public record GuardarIpCommand(
            String ip,
            String equipo,
            String detalle,
            String observaciones,
            CategoriaIp categoria,
            EstadoIp estado
    ) {}

    public record ValidacionIpResult(
            boolean ocupada,
            String equipo,
            String detalle,
            String observaciones,
            EstadoIp estado
    ) {}

    public record MetricasRedIp(
            long total,
            long ocupadas,
            long libres,
            long reservadas,
            int porcentajeUso
    ) {}
}
