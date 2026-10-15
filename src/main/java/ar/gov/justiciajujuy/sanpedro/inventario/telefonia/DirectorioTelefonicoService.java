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

	private final DirectorioTelefonicoRepository repository;

	// Fallback en memoria con internos iniciales conocidos
	private final Map<String, String> cacheInternos = new ConcurrentHashMap<>();

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

	@Transactional(readOnly = true)
	public String resolverNombre(String numero) {
		if (!StringUtils.hasText(numero)) {
			return "";
		}
		String num = numero.trim();
		if (repository != null) {
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
				return repository.findAllByOrderByNumeroAsc();
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
