package ar.gov.justiciajujuy.sanpedro.inventario.armado;

import java.time.LocalDateTime;

import ar.gov.justiciajujuy.sanpedro.inventario.equipos.Equipo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ordenes_armado")
public class OrdenArmado {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "equipo_id", nullable = false)
	private Equipo equipo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private EstadoOrdenArmado estado = EstadoOrdenArmado.BORRADOR;

	@Column(nullable = false, length = 255)
	private String descripcion;

	@Column(length = 500)
	private String observaciones;

	@Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
	private LocalDateTime creadoEn;

	@Column(name = "actualizado_en", nullable = false, insertable = false, updatable = false)
	private LocalDateTime actualizadoEn;

	protected OrdenArmado() {
	}

	public OrdenArmado(Equipo equipo, String descripcion) {
		this.equipo = equipo;
		this.descripcion = descripcion;
	}

	public void actualizar(EstadoOrdenArmado estado, String descripcion, String observaciones) {
		this.estado = estado;
		this.descripcion = descripcion;
		this.observaciones = observaciones;
	}

	public Long getId() {
		return id;
	}

	public Equipo getEquipo() {
		return equipo;
	}

	public EstadoOrdenArmado getEstado() {
		return estado;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public String getObservaciones() {
		return observaciones;
	}
}
