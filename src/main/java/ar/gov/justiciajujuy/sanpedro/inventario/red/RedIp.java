package ar.gov.justiciajujuy.sanpedro.inventario.red;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "red_ips")
public class RedIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 45)
    private String ip;

    @Column(name = "ip_numero", nullable = false)
    private Integer ipNumero;

    @Column(nullable = false, length = 45)
    private String subred = "10.15.0.0/24";

    @Column(length = 150)
    private String equipo;

    @Column(length = 255)
    private String detalle;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private CategoriaIp categoria = CategoriaIp.OTRO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoIp estado = EstadoIp.LIBRE;

    @Column(name = "actualizado_por", length = 120)
    private String actualizadoPor;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

    public RedIp() {
    }

    public RedIp(String ip, Integer ipNumero, String subred, String equipo, String detalle, String observaciones, CategoriaIp categoria, EstadoIp estado, String actualizadoPor) {
        this.ip = ip;
        this.ipNumero = ipNumero;
        this.subred = subred != null ? subred : "10.15.0.0/24";
        this.equipo = equipo;
        this.detalle = detalle;
        this.observaciones = observaciones;
        this.categoria = categoria != null ? categoria : CategoriaIp.OTRO;
        this.estado = estado != null ? estado : EstadoIp.LIBRE;
        this.actualizadoPor = actualizadoPor;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getIpNumero() {
        return ipNumero;
    }

    public void setIpNumero(Integer ipNumero) {
        this.ipNumero = ipNumero;
    }

    public String getSubred() {
        return subred;
    }

    public void setSubred(String subred) {
        this.subred = subred;
    }

    public String getEquipo() {
        return equipo;
    }

    public void setEquipo(String equipo) {
        this.equipo = equipo;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public CategoriaIp getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaIp categoria) {
        this.categoria = categoria;
    }

    public EstadoIp getEstado() {
        return estado;
    }

    public void setEstado(EstadoIp estado) {
        this.estado = estado;
    }

    public String getActualizadoPor() {
        return actualizadoPor;
    }

    public void setActualizadoPor(String actualizadoPor) {
        this.actualizadoPor = actualizadoPor;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
