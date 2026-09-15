package ar.gov.justiciajujuy.sanpedro.inventario.tareas;

import java.time.LocalDateTime;

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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "tareas_stock_usos")
public class TareaStockUso {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tarea_id", nullable = false)
	private TareaTecnica tarea;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stock_componente_id", nullable = false)
	private StockComponente stockComponente;

	@Column(name = "registrado_por", nullable = false, length = 120)
	private String registradoPor;

	@Column(length = 500)
	private String observacion;

	@Column(name = "desvinculado_en")
	private LocalDateTime desvinculadoEn;

	@Column(name = "desvinculado_por", length = 120)
	private String desvinculadoPor;

	@Column(name = "desvinculacion_motivo", length = 500)
	private String desvinculacionMotivo;

	@Column(name = "desvinculado_equipo_nombre", length = 180)
	private String desvinculadoEquipoNombre;

	@CreationTimestamp
	@Column(name = "creado_en", nullable = false, updatable = false)
	private LocalDateTime creadoEn;

	protected TareaStockUso() {
	}

	public TareaStockUso(TareaTecnica tarea, StockComponente stockComponente, String registradoPor, String observacion) {
		this.tarea = tarea;
		this.stockComponente = stockComponente;
		this.registradoPor = registradoPor;
		this.observacion = observacion;
	}

	public Long getId() { return id; }
	public TareaTecnica getTarea() { return tarea; }
	public StockComponente getStockComponente() { return stockComponente; }
	public String getRegistradoPor() { return registradoPor; }
	public String getObservacion() { return observacion; }
	public LocalDateTime getDesvinculadoEn() { return desvinculadoEn; }
	public String getDesvinculadoPor() { return desvinculadoPor; }
	public String getDesvinculacionMotivo() { return desvinculacionMotivo; }
	public String getDesvinculadoEquipoNombre() { return desvinculadoEquipoNombre; }
	public LocalDateTime getCreadoEn() { return creadoEn; }

	public void registrarDesvinculacion(String usuario, String motivo, String equipoNombre) {
		this.desvinculadoEn = LocalDateTime.now();
		this.desvinculadoPor = usuario;
		this.desvinculacionMotivo = motivo;
		this.desvinculadoEquipoNombre = equipoNombre;
	}
}
