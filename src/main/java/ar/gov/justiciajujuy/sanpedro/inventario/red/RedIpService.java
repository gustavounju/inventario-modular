package ar.gov.justiciajujuy.sanpedro.inventario.red;

import ar.gov.justiciajujuy.sanpedro.inventario.equipos.Equipo;
import ar.gov.justiciajujuy.sanpedro.inventario.equipos.EquipoRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RedIpService {

    private static final Logger log = LoggerFactory.getLogger(RedIpService.class);

    private final RedIpRepository repository;
    private final EquipoRepository equipoRepository;

    public RedIpService(RedIpRepository repository, EquipoRepository equipoRepository) {
        this.repository = repository;
        this.equipoRepository = equipoRepository;
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

    /**
     * Cruza el plano IPAM (tabla red_ips) contra las IPs reales detectadas por el script de inventario
     * (equipos e impresoras de red) para detectar colisiones o IPs que figuran LIBRES pero están en uso.
     */
    @Transactional(readOnly = true)
    public List<DiscrepanciaIp> detectarDiscrepanciasConInventario() {
        List<Equipo> equipos = equipoRepository.findAllByOrderByNombreAsc();
        Map<String, RedIp> mapaIpam = repository.findAllByOrderByIpNumeroAsc().stream()
                .collect(Collectors.toMap(RedIp::getIp, r -> r, (a, b) -> a));

        Map<String, List<Equipo>> equiposPorIp = new LinkedHashMap<>();
        for (Equipo eq : equipos) {
            if (!eq.isActivo() || "PC-GENERICA".equalsIgnoreCase(eq.getNombre())) {
                continue;
            }
            String ip = eq.getIp() != null ? eq.getIp().trim() : "";
            if (esIpAuditableJudicial(ip)) {
                equiposPorIp.computeIfAbsent(ip, k -> new ArrayList<>()).add(eq);
            }
            // Si una PC reportó una IP de impresora de red y aún no está reflejada
            String impIp = eq.getImpresoraIp() != null ? eq.getImpresoraIp().trim() : "";
            if (esIpAuditableJudicial(impIp) && !impIp.equals(ip)) {
                equiposPorIp.computeIfAbsent(impIp, k -> new ArrayList<>());
            }
        }

        List<DiscrepanciaIp> discrepancias = new ArrayList<>();
        for (Map.Entry<String, List<Equipo>> entry : equiposPorIp.entrySet()) {
            String ip = entry.getKey();
            List<Equipo> hosts = entry.getValue();
            if (hosts.isEmpty()) {
                continue;
            }
            RedIp registroIpam = mapaIpam.get(ip);

            if (hosts.size() > 1) {
                String nombres = hosts.stream().map(Equipo::getNombre).collect(Collectors.joining(", "));
                Equipo primero = hosts.get(0);
                discrepancias.add(new DiscrepanciaIp(
                        ip,
                        "DUPLICADA_EN_INVENTARIO",
                        "🚨 Colisión: " + hosts.size() + " equipos reportan la misma IP (" + nombres + ")",
                        primero.getId(),
                        primero.getNombre(),
                        primero.getFuero(),
                        primero.getUbicacion(),
                        registroIpam != null ? registroIpam.getEquipo() : null,
                        registroIpam != null ? registroIpam.getEstado() : null,
                        esImpresora(primero) ? CategoriaIp.OTRO : CategoriaIp.TERMINAL));
                continue;
            }

            Equipo host = hosts.get(0);
            CategoriaIp catSugerida = esImpresora(host) ? CategoriaIp.OTRO : CategoriaIp.TERMINAL;
            if (registroIpam == null) {
                discrepancias.add(new DiscrepanciaIp(
                        ip,
                        "NO_REGISTRADA_EN_PLANO",
                        "⚠️ IP activa en inventario pero ausente en el plano IPAM",
                        host.getId(),
                        host.getNombre(),
                        host.getFuero(),
                        host.getUbicacion(),
                        null,
                        null,
                        catSugerida));
            } else if (registroIpam.getEstado() == EstadoIp.LIBRE) {
                discrepancias.add(new DiscrepanciaIp(
                        ip,
                        "FIGURA_LIBRE_PERO_USADA",
                        "⚠️ Figura LIBRE en el plano pero el equipo " + host.getNombre() + " la está usando",
                        host.getId(),
                        host.getNombre(),
                        host.getFuero(),
                        host.getUbicacion(),
                        registroIpam.getEquipo(),
                        registroIpam.getEstado(),
                        catSugerida));
            } else if (registroIpam.getEquipo() != null
                    && !registroIpam.getEquipo().isBlank()
                    && !coincideNombreEquipo(registroIpam.getEquipo(), host.getNombre())) {
                discrepancias.add(new DiscrepanciaIp(
                        ip,
                        "DIFERENTE_EQUIPO",
                        "🔄 En plano figura '" + registroIpam.getEquipo() + "' pero el script reporta '" + host.getNombre() + "'",
                        host.getId(),
                        host.getNombre(),
                        host.getFuero(),
                        host.getUbicacion(),
                        registroIpam.getEquipo(),
                        registroIpam.getEstado(),
                        catSugerida));
            }
        }
        return discrepancias;
    }

    private boolean esImpresora(Equipo host) {
        return "Impresora de Red".equalsIgnoreCase(host.getSistemaOperativo());
    }

    private boolean esIpAuditableJudicial(String ip) {
        if (ip == null || ip.isBlank() || "Sin IP".equalsIgnoreCase(ip) || "127.0.0.1".equals(ip)) {
            return false;
        }
        // Solo auditar la VLAN/subred LAN monitoreada del Juzgado: 10.15.0.0/24 (10.15.0.1 a 10.15.0.254)
        // Ignora cualquier otra IP fuera de esta subred
        return ip.matches("^10\\.15\\.0\\.\\d{1,3}$");
    }

    private boolean coincideNombreEquipo(String nombrePlano, String nombreInventario) {
        if (nombrePlano == null || nombreInventario == null) {
            return false;
        }
        String a = nombrePlano.trim().toLowerCase();
        String b = nombreInventario.trim().toLowerCase();
        return a.equals(b) || a.contains(b) || b.contains(a);
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

    public record DiscrepanciaIp(
            String ip,
            String tipo,
            String descripcion,
            Long equipoId,
            String equipoInventario,
            String fuero,
            String ubicacion,
            String equipoPlano,
            EstadoIp estadoPlano,
            CategoriaIp categoriaSugerida
    ) {}
}
