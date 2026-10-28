package ar.gov.justiciajujuy.sanpedro.inventario.equipos;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Representa una estación de trabajo (PC, notebook, servidor) dentro del inventario del Centro Judicial.
 * Puede originarse por relevamiento automatizado vía script o por ensamblado inicial en el Taller de Informática.
 */
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

	@Column(length = 180)
	private String ubicacion;

	@Column(length = 45)
	private String ip;

	@Column(length = 45)
	private String mac;

	@Column(name = "sistema_operativo", length = 180)
	private String sistemaOperativo;

	@Column(length = 255)
	private String procesador;

	@Column(name = "ram_mb")
	private Integer ramMb;

	@Column(name = "ram_detalles", length = 500)
	private String ramDetalles;

	@Column(name = "ram_seriales", length = 500)
	private String ramSeriales;

	@Column(name = "discos_modelos", length = 500)
	private String discosModelos;

	@Column(name = "discos_seriales", length = 500)
	private String discosSeriales;

	@Column(name = "motherboard_modelo", length = 255)
	private String motherboardModelo;

	@Column(name = "motherboard_serial", length = 255)
	private String motherboardSerial;

	@Column(length = 500)
	private String monitores;

	@Column(length = 180)
	private String teclado;

	@Column(length = 180)
	private String mouse;

	@Column(length = 180)
	private String impresora;

	@Column(name = "impresora_ip", length = 45)
	private String impresoraIp;

	@Column(name = "impresora_en_red", nullable = false)
	private boolean impresoraEnRed = false;

	@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
	@jakarta.persistence.JoinColumn(name = "impresora_red_id")
	private Equipo impresoraRedAsignada;

	@Column(nullable = false, length = 60)
	private String monitoreo = "SIN_REPORTE";

	@Column(nullable = false)
	private boolean activo = true;

	@jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
	@Column(name = "estado_ciclo", nullable = false, length = 40)
	private EstadoCicloEquipo estadoCiclo = EstadoCicloEquipo.EN_SERVICIO;

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

	/**
	 * Fábrica para instanciar un equipo en estado inicial de ensamblado en Taller.
	 * Se crea con fuero y ubicación fijados en 'Taller de Informática', usuario 'Sin asignar'
	 * y sistema operativo 'Pendiente de instalación / relevamiento' hasta que sea entregado
	 * a su juzgado destino y relevado por el script automatizado.
	 *
	 * @param nombre Código identificador provisorio o definitivo (ej. ARMADO-001)
	 * @return Instancia de Equipo lista para asociar piezas de Stock en una Orden de Armado
	 */
	public static Equipo crearParaTaller(String nombre) {
		Equipo equipo = new Equipo(nombre, "Taller de Informática");
		equipo.ubicacion = "Taller de Informática";
		equipo.ultimoUsuario = null;
		equipo.sistemaOperativo = "Pendiente de instalación / relevamiento";
		equipo.activo = true;
		equipo.estadoCiclo = EstadoCicloEquipo.EN_ARMADO;
		return equipo;
	}

	public EstadoCicloEquipo getEstadoCiclo() {
		return estadoCiclo;
	}

	/**
	 * El armado termino: el equipo queda en el taller a la espera de ser entregado.
	 *
	 * @return true si hubo transicion (solo desde EN_ARMADO o EN_REPARACION)
	 */
	public boolean marcarDisponibleEnTaller() {
		if (estadoCiclo != EstadoCicloEquipo.EN_ARMADO && estadoCiclo != EstadoCicloEquipo.EN_REPARACION) {
			return false;
		}
		this.estadoCiclo = EstadoCicloEquipo.DISPONIBLE;
		return true;
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

	public void setFuero(String fuero) {
		this.fuero = fuero;
	}

	public String getUbicacion() {
		return ubicacion;
	}

	public void setUbicacion(String ubicacion) {
		this.ubicacion = ubicacion;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
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

	public String getRamDetalles() {
		return ramDetalles;
	}

	public String getRamSeriales() {
		return ramSeriales;
	}

	public String getDiscosModelos() {
		return discosModelos;
	}

	public String getDiscosSeriales() {
		return discosSeriales;
	}

	public String getMotherboardModelo() {
		return motherboardModelo;
	}

	public String getMotherboardSerial() {
		return motherboardSerial;
	}

	public void vincularSerial(String serial) {
		this.motherboardSerial = serial;
	}

	public String getMonitores() {
		return monitores;
	}

	public String getTeclado() {
		return teclado;
	}

	public String getMouse() {
		return mouse;
	}

	public String getImpresora() {
		return impresora;
	}

	public void setImpresora(String impresora) {
		this.impresora = impresora;
	}

	public String getImpresoraIp() {
		return impresoraIp;
	}

	public void setImpresoraIp(String impresoraIp) {
		this.impresoraIp = impresoraIp;
	}

	public boolean isImpresoraEnRed() {
		return impresoraEnRed;
	}

	public void setImpresoraEnRed(boolean impresoraEnRed) {
		this.impresoraEnRed = impresoraEnRed;
	}

	public Equipo getImpresoraRedAsignada() {
		return impresoraRedAsignada;
	}

	public void setImpresoraRedAsignada(Equipo impresoraRedAsignada) {
		this.impresoraRedAsignada = impresoraRedAsignada;
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
			String ubicacion,
			String ip,
			String mac,
			String sistemaOperativo,
			String procesador,
			Integer ramMb,
			String ramDetalles,
			String ramSeriales,
			String discosModelos,
			String discosSeriales,
			String motherboardModelo,
			String motherboardSerial,
			String monitores,
			String teclado,
			String mouse,
			String impresora,
			String impresoraIp,
			Boolean impresoraEnRed,
			boolean activo,
			LocalDateTime reportadoEn) {
		this.ultimoUsuario = ultimoUsuario;
		this.fuero = fuero;
		this.ubicacion = ubicacion;
		this.ip = ip;
		this.mac = mac;
		this.sistemaOperativo = sistemaOperativo;
		this.procesador = procesador;
		this.ramMb = ramMb;
		this.ramDetalles = ramDetalles;
		this.ramSeriales = ramSeriales;
		this.discosModelos = discosModelos;
		this.discosSeriales = discosSeriales;
		this.motherboardModelo = motherboardModelo;
		this.motherboardSerial = motherboardSerial;
		this.monitores = monitores;
		this.teclado = teclado;
		this.mouse = mouse;
		this.impresora = impresora;
		this.impresoraIp = impresoraIp;
		if (impresoraEnRed != null) {
			this.impresoraEnRed = impresoraEnRed;
		}
		if (this.impresoraRedAsignada != null) {
			if ((this.impresoraRedAsignada.getIp() == null || this.impresoraRedAsignada.getIp().isBlank() || "Sin IP".equalsIgnoreCase(this.impresoraRedAsignada.getIp())) && (impresoraIp != null && !impresoraIp.isBlank())) {
				this.impresoraRedAsignada.setIp(impresoraIp);
			}
			if ((this.impresoraRedAsignada.getFuero() == null || this.impresoraRedAsignada.getFuero().isBlank() || "Desconocido".equalsIgnoreCase(this.impresoraRedAsignada.getFuero())) && (fuero != null && !fuero.isBlank() && !"Desconocido".equalsIgnoreCase(fuero))) {
				this.impresoraRedAsignada.setFuero(fuero);
			}
			if ((this.impresoraRedAsignada.getUbicacion() == null || this.impresoraRedAsignada.getUbicacion().isBlank() || "Sin ubicación asignada".equalsIgnoreCase(this.impresoraRedAsignada.getUbicacion()) || "Mesa de Entradas".equalsIgnoreCase(this.impresoraRedAsignada.getUbicacion())) && (ubicacion != null && !ubicacion.isBlank() && !"Sin ubicación asignada".equalsIgnoreCase(ubicacion))) {
				this.impresoraRedAsignada.setUbicacion(ubicacion);
			}
		}
		this.activo = activo;
		this.monitoreo = "REPORTADO";
		this.ultimoReporteEn = reportadoEn;
	}

	public void actualizarManualmente(
			String nombre,
			String ultimoUsuario,
			String fuero,
			String ubicacion,
			String ip,
			String mac,
			String sistemaOperativo,
			String procesador,
			Integer ramMb,
			String ramDetalles,
			String ramSeriales,
			String discosModelos,
			String discosSeriales,
			String motherboardModelo,
			String motherboardSerial,
			String monitores,
			String teclado,
			String mouse,
			String impresora,
			boolean activo) {
		this.nombre = nombre;
		this.ultimoUsuario = ultimoUsuario;
		this.fuero = fuero;
		this.ubicacion = ubicacion;
		this.ip = ip;
		this.mac = mac;
		this.sistemaOperativo = sistemaOperativo;
		this.procesador = procesador;
		this.ramMb = ramMb;
		this.ramDetalles = ramDetalles;
		this.ramSeriales = ramSeriales;
		this.discosModelos = discosModelos;
		this.discosSeriales = discosSeriales;
		this.motherboardModelo = motherboardModelo;
		this.motherboardSerial = motherboardSerial;
		this.monitores = monitores;
		this.teclado = teclado;
		this.mouse = mouse;
		this.impresora = impresora;
		this.activo = activo;
	}
}
