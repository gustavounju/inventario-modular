package ar.gov.justiciajujuy.sanpedro.inventario.telefonia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectorioTelefonicoRepository extends JpaRepository<DirectorioTelefonico, Long> {

	Optional<DirectorioTelefonico> findByNumero(String numero);

	List<DirectorioTelefonico> findAllByOrderByNumeroAsc();
}
