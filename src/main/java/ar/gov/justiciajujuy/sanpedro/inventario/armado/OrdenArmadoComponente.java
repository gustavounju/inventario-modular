package ar.gov.justiciajujuy.sanpedro.inventario.armado;

import java.time.LocalDateTime;

import ar.gov.justiciajujuy.sanpedro.inventario.componentes.Componente;
import ar.gov.justiciajujuy.sanpedro.inventario.stock.StockComponente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "orden_armado_componentes")
public class OrdenArmadoComponente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "orden_id", nullable = false)
	private OrdenArmado orden;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "componente_id", nullable = false)
	private Componente componente;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stock_componente_id")
	private StockComponente stockComponente;

	@Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
	private LocalDateTime creadoEn;

	protected OrdenArmadoComponente() {
	}

	public OrdenArmadoComponente(OrdenArmado orden, Componente componente, StockComponente stockComponente) {
		this.orden = orden;
		this.componente = componente;
		this.stockComponente = stockComponente;
	}

	public Long getId() {
		return id;
	}

	public OrdenArmado getOrden() {
		return orden;
	}

	public Componente getComponente() {
		return componente;
	}

	public StockComponente getStockComponente() {
		return stockComponente;
	}
}
