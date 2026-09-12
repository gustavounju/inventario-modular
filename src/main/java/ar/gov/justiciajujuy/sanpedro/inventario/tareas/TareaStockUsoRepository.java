package ar.gov.justiciajujuy.sanpedro.inventario.tareas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TareaStockUsoRepository extends JpaRepository<TareaStockUso, Long> {

	List<TareaStockUso> findByTareaIdOrderByCreadoEnDescIdDesc(Long tareaId);
}
