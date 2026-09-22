package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioSistemaRepository extends JpaRepository<UsuarioSistema, Long> {

	Optional<UsuarioSistema> findByUsernameIgnoreCase(String username);

	boolean existsByUsernameIgnoreCase(String username);

	@Query("""
			SELECT DISTINCT u
			FROM UsuarioSistema u
			LEFT JOIN FETCH u.roles
			ORDER BY u.username
			""")
	java.util.List<UsuarioSistema> findAllWithRoles();

	/**
	 * Cuenta la cantidad de usuarios activos en el sistema que poseen el rol de ADMINISTRADOR.
	 * Utilizado principalmente para validar que el sistema no se quede sin administradores
	 * o para forzar la configuración inicial si el conteo es cero.
	 */
	@Query("SELECT COUNT(u) FROM UsuarioSistema u JOIN u.roles r WHERE r.codigo = 'ADMINISTRADOR' AND u.activo = true")
	long countAdministradores();
}
