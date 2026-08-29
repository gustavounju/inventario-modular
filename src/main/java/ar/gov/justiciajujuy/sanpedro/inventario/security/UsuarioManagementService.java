package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioManagementService {

	private final UsuarioSistemaRepository usuarioSistemaRepository;
	private final RolRepository rolRepository;

	public UsuarioManagementService(UsuarioSistemaRepository usuarioSistemaRepository, RolRepository rolRepository) {
		this.usuarioSistemaRepository = usuarioSistemaRepository;
		this.rolRepository = rolRepository;
	}

	@Transactional(readOnly = true)
	public List<UsuarioResumen> listarUsuarios() {
		return usuarioSistemaRepository.findAllWithRoles().stream()
				.map(this::toResumen)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<RolResumen> listarRoles() {
		return rolRepository.findAll().stream()
				.sorted(java.util.Comparator.comparing(Rol::getCodigo))
				.map(rol -> new RolResumen(
						rol.getId(),
						rol.getCodigo(),
						rol.getNombre(),
						rol.getDescripcion(),
						rol.isActivo()))
				.toList();
	}

	@Transactional
	public UsuarioResumen crearUsuario(CrearUsuarioCommand command) {
		/*
		 * El username se guarda normalizado para que `GMURAD`, `gmurad` y `Gmurad`
		 * representen a la misma cuenta de dominio dentro del inventario.
		 */
		String usernameNormalizado = normalizar(command.username());
		if (usuarioSistemaRepository.existsByUsernameIgnoreCase(usernameNormalizado)) {
			throw new UsuarioDuplicadoException(usernameNormalizado);
		}

		UsuarioSistema usuario = new UsuarioSistema(
				usernameNormalizado,
				command.nombreVisible().trim(),
				command.fuero().trim());
		usuario.setActivo(command.activo());
		usuario.reemplazarRoles(buscarRoles(command.roles()));

		return toResumen(usuarioSistemaRepository.save(usuario));
	}

	private Set<Rol> buscarRoles(Set<String> codigos) {
		Set<String> codigosNormalizados = new TreeSet<>();
		for (String codigo : codigos) {
			codigosNormalizados.add(codigo.trim().toUpperCase());
		}

		Set<Rol> roles = rolRepository.findByCodigoInAndActivoTrue(codigosNormalizados);
		if (roles.size() != codigosNormalizados.size()) {
			throw new RolNoEncontradoException(codigosNormalizados);
		}
		return roles;
	}

	private UsuarioResumen toResumen(UsuarioSistema usuario) {
		List<String> roles = usuario.getRoles().stream()
				.map(Rol::getCodigo)
				.sorted()
				.toList();
		return new UsuarioResumen(
				usuario.getId(),
				usuario.getUsername(),
				usuario.getNombreVisible(),
				usuario.getFuero(),
				usuario.isActivo(),
				roles);
	}

	private String normalizar(String username) {
		return username.trim().toLowerCase();
	}

	public record CrearUsuarioCommand(
			String username,
			String nombreVisible,
			String fuero,
			boolean activo,
			Set<String> roles) {
	}

	public record UsuarioResumen(
			Long id,
			String username,
			String nombreVisible,
			String fuero,
			boolean activo,
			List<String> roles) {
	}

	public record RolResumen(
			Long id,
			String codigo,
			String nombre,
			String descripcion,
			boolean activo) {
	}

	public static class UsuarioDuplicadoException extends RuntimeException {

		public UsuarioDuplicadoException(String username) {
			super("El usuario ya existe: " + username);
		}
	}

	public static class RolNoEncontradoException extends RuntimeException {

		public RolNoEncontradoException(Set<String> codigos) {
			super("No se encontraron todos los roles solicitados: " + codigos);
		}
	}
}
