package ar.gov.justiciajujuy.sanpedro.inventario.equipos;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "equipos")
public class Equipo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String nombre;

	@Column(name = "ultimo_usuario", length = 120)
	private String ultimoUsuario;

	@Column(nullable = false, length = 120)
	private String fuero;

	@Column(length = 45)
	private String ip;

	@Column(name = "sistema_operativo", length = 180)
	private String sistemaOperativo;

	@Column(length = 255)
	private String procesador;

	@Column(name = "ram_mb")
	private Integer ramMb;

	@Column(length = 180)
	private String impresora;

	@Column(nullable = false, length = 60)
	private String monitoreo = "SIN_REPORTE";

	@Column(nullable = false)
	private boolean activo = true;

	@Column(name = "ultimo_reporte_en")
	private LocalDateTime ultimoReporteEn;

	@Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
	private LocalDateTime creadoEn;

	@Column(name = "actualizado_en", nullable = false, insertable = false, updatable = false)
	private LocalDateTime actualizadoEn;

	protected Equipo() {
	}

	public Equipo(String nombre, String fuero) {
		this.nombre = nombre;
		this.fuero = fuero;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getUltimoUsuario() {
		return ultimoUsuario;
	}

	public String getFuero() {
		return fuero;
	}

	public String getIp() {
		return ip;
	}

	public String getSistemaOperativo() {
		return sistemaOperativo;
	}

	public String getProcesador() {
		return procesador;
	}

	public Integer getRamMb() {
		return ramMb;
	}

	public String getImpresora() {
		return impresora;
	}

	public String getMonitoreo() {
		return monitoreo;
	}

	public boolean isActivo() {
		return activo;
	}

	public LocalDateTime getUltimoReporteEn() {
		return ultimoReporteEn;
	}

	public void actualizarDesdeReporte(
			String ultimoUsuario,
			String fuero,
			String ip,
			String sistemaOperativo,
			String procesador,
			Integer ramMb,
			String impresora,
			boolean activo,
			LocalDateTime reportadoEn) {
		this.ultimoUsuario = ultimoUsuario;
		this.fuero = fuero;
		this.ip = ip;
		this.sistemaOperativo = sistemaOperativo;
		this.procesador = procesador;
		this.ramMb = ramMb;
		this.impresora = impresora;
		this.activo = activo;
		this.monitoreo = "REPORTADO";
		this.ultimoReporteEn = reportadoEn;
	}
}
