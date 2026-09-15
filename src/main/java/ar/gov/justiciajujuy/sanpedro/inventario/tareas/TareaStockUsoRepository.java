package ar.gov.justiciajujuy.sanpedro.inventario.tareas;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TareaStockUsoRepository extends JpaRepository<TareaStockUso, Long> {

	List<TareaStockUso> findByTareaIdOrderByCreadoEnDescIdDesc(Long tareaId);

	Optional<TareaStockUso> findByIdAndTareaId(Long id, Long tareaId);

	Optional<TareaStockUso> findFirstByStockComponenteIdOrderByCreadoEnDescIdDesc(Long stockComponenteId);

	List<TareaStockUso> findByStockComponenteIdOrderByCreadoEnDescIdDesc(Long stockComponenteId);
}
