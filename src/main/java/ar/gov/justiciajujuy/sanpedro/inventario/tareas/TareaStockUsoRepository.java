package ar.gov.justiciajujuy.sanpedro.inventario.tareas;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TareaStockUsoRepository extends JpaRepository<TareaStockUso, Long> {

	List<TareaStockUso> findByTareaIdOrderByCreadoEnDescIdDesc(Long tareaId);

	@Query("""
			SELECT u
			FROM TareaStockUso u
			JOIN FETCH u.stockComponente sc
			JOIN FETCH u.tarea t
			WHERE t.id IN :tareaIds
			ORDER BY u.creadoEn DESC, u.id DESC
			""")
	List<TareaStockUso> buscarPorTareas(@Param("tareaIds") Collection<Long> tareaIds);

	Optional<TareaStockUso> findByIdAndTareaId(Long id, Long tareaId);

	Optional<TareaStockUso> findFirstByStockComponenteIdOrderByCreadoEnDescIdDesc(Long stockComponenteId);

	List<TareaStockUso> findByStockComponenteIdOrderByCreadoEnDescIdDesc(Long stockComponenteId);
}
