package ar.gov.justiciajujuy.sanpedro.inventario.telefonia;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "directorio_telefonico")
public class DirectorioTelefonico {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String numero;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(length = 120)
	private String departamento;

	@Column(nullable = false, length = 30)
	private String origen = "MANUAL";

	@Column(name = "creado_en", insertable = false, updatable = false)
	private LocalDateTime creadoEn;

	@Column(name = "actualizado_en", insertable = false, updatable = false)
	private LocalDateTime actualizadoEn;

	protected DirectorioTelefonico() {
	}

	public DirectorioTelefonico(String numero, String nombre, String departamento, String origen) {
		this.numero = numero != null ? numero.trim() : "";
		this.nombre = nombre != null ? nombre.trim() : "";
		this.departamento = departamento != null ? departamento.trim() : "";
		this.origen = origen != null ? origen.trim() : "MANUAL";
	}

	public Long getId() {
		return id;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero != null ? numero.trim() : "";
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre != null ? nombre.trim() : "";
	}

	public String getDepartamento() {
		return departamento;
	}

	public void setDepartamento(String departamento) {
		this.departamento = departamento != null ? departamento.trim() : "";
	}

	public String getOrigen() {
		return origen;
	}

	public void setOrigen(String origen) {
		this.origen = origen != null ? origen.trim() : "MANUAL";
	}

	public LocalDateTime getCreadoEn() {
		return creadoEn;
	}

	public LocalDateTime getActualizadoEn() {
		return actualizadoEn;
	}
}
