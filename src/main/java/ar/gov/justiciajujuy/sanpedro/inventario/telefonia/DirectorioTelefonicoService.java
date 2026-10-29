package ar.gov.justiciajujuy.sanpedro.inventario.telefonia;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DirectorioTelefonicoService {

	private static final Logger log = LoggerFactory.getLogger(DirectorioTelefonicoService.class);
	private static final long CACHE_WARM_TTL_MS = 120_000L;

	private final DirectorioTelefonicoRepository repository;

	// Fallback y caché en memoria con internos conocidos
	private final Map<String, String> cacheInternos = new ConcurrentHashMap<>();
	private volatile long cacheLoadedUntilMs = 0L;

	@Autowired
	public DirectorioTelefonicoService(@Autowired(required = false) DirectorioTelefonicoRepository repository) {
		this.repository = repository;
		// Precargar internos institucionales conocidos
		cacheInternos.put("2005", "MesaDeEntradasInformatica");
		cacheInternos.put("2000", "Mesa de Entradas General");
		cacheInternos.put("1005", "Taller Informatica");
	}

	public DirectorioTelefonicoService() {
		this(null);
	}

	private void ensureCacheWarm() {
		if (repository == null) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now < cacheLoadedUntilMs) {
			return;
		}
		try {
			List<DirectorioTelefonico> all = repository.findAllByOrderByNumeroAsc();
			for (DirectorioTelefonico entry : all) {
				if (entry != null && StringUtils.hasText(entry.getNumero()) && StringUtils.hasText(entry.getNombre())) {
					cacheInternos.put(entry.getNumero().trim(), entry.getNombre().trim());
				}
			}
			cacheLoadedUntilMs = now + CACHE_WARM_TTL_MS;
		} catch (Exception e) {
			log.debug("Error precargando directorio telefónico en memoria: {}", e.getMessage());
		}
	}

	@Transactional(readOnly = true)
	public String resolverNombre(String numero) {
		if (!StringUtils.hasText(numero)) {
			return "";
		}
		String num = numero.trim();
		ensureCacheWarm();
		String cached = cacheInternos.get(num);
		if (StringUtils.hasText(cached)) {
			return cached;
		}
		if (repository != null && cacheLoadedUntilMs == 0L) {
			try {
				Optional<DirectorioTelefonico> opt = repository.findByNumero(num);
				if (opt.isPresent() && StringUtils.hasText(opt.get().getNombre())) {
					cacheInternos.put(num, opt.get().getNombre());
					return opt.get().getNombre();
				}
			} catch (Exception e) {
				log.debug("Error consultando directorio telefónico en BD: {}", e.getMessage());
			}
		}
		return cacheInternos.getOrDefault(num, "");
	}

	/**
	 * Formatea una representación clara del teléfono: número + nombre asignado.
	 * Ejemplo: "2005 MesaDeEntradasInformatica"
	 */
	@Transactional(readOnly = true)
	public String formatearNumeroYNombre(String numero, String nombreExistente) {
		if (!StringUtils.hasText(numero)) {
			return StringUtils.hasText(nombreExistente) ? nombreExistente.trim() : "Desconocido";
		}
		String num = numero.trim();
		String nombre = (StringUtils.hasText(nombreExistente) && !nombreExistente.trim().equalsIgnoreCase(num))
				? nombreExistente.trim()
				: resolverNombre(num);

		if (StringUtils.hasText(nombre)) {
			return num + " " + nombre;
		}
		return num;
	}

	@Transactional
	public DirectorioTelefonico guardarOActualizar(String numero, String nombre, String departamento, String origen) {
		if (!StringUtils.hasText(numero) || !StringUtils.hasText(nombre)) {
			throw new IllegalArgumentException("El número y el nombre del interno son obligatorios.");
		}
		String num = numero.trim();
		String nom = nombre.trim();
		String dep = departamento != null ? departamento.trim() : "";
		String orig = StringUtils.hasText(origen) ? origen.trim() : "MANUAL";

		cacheInternos.put(num, nom);

		if (repository != null) {
			DirectorioTelefonico entry = repository.findByNumero(num)
					.orElseGet(() -> new DirectorioTelefonico(num, nom, dep, orig));
			entry.setNombre(nom);
			entry.setDepartamento(dep);
			entry.setOrigen(orig);
			return repository.save(entry);
		}
		return new DirectorioTelefonico(num, nom, dep, orig);
	}

	@Transactional(readOnly = true)
	public List<DirectorioTelefonico> listarTodos() {
		if (repository != null) {
			try {
				List<DirectorioTelefonico> list = repository.findAllByOrderByNumeroAsc();
				for (DirectorioTelefonico entry : list) {
					if (entry != null && StringUtils.hasText(entry.getNumero()) && StringUtils.hasText(entry.getNombre())) {
						cacheInternos.put(entry.getNumero().trim(), entry.getNombre().trim());
					}
				}
				cacheLoadedUntilMs = System.currentTimeMillis() + CACHE_WARM_TTL_MS;
				return list;
			} catch (Exception e) {
				log.warn("No se pudo listar el directorio telefónico desde la BD: {}", e.getMessage());
			}
		}
		return Collections.emptyList();
	}

	@Transactional
	public void eliminar(Long id) {
		if (repository != null && id != null) {
			repository.findById(id).ifPresent(e -> {
				cacheInternos.remove(e.getNumero());
				repository.delete(e);
			});
		}
	}
}
