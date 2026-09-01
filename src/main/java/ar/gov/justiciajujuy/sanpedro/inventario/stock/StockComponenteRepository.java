package ar.gov.justiciajujuy.sanpedro.inventario.stock;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockComponenteRepository extends JpaRepository<StockComponente, Long> {

	List<StockComponente> findByActivoTrueOrderByTipoAscDescripcionAsc();
}
