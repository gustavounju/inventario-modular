package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioSistemaRepository extends JpaRepository<UsuarioSistema, Long> {

	Optional<UsuarioSistema> findByUsernameIgnoreCase(String username);
}
