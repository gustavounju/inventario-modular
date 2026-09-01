package ar.gov.justiciajujuy.sanpedro.inventario.auditoria;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {

	List<AuditoriaEvento> findTop100ByOrderByIdDesc();
}
