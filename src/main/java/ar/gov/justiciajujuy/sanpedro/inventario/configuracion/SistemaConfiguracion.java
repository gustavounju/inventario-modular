package ar.gov.justiciajujuy.sanpedro.inventario.configuracion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sistema_configuracion")
public class SistemaConfiguracion {

	@Id
	@Column(name = "clave", length = 120, nullable = false)
	private String clave;

	@Column(name = "valor", columnDefinition = "TEXT")
	private String valor;

	protected SistemaConfiguracion() {
	}

	public SistemaConfiguracion(String clave, String valor) {
		this.clave = clave;
		this.valor = valor;
	}

	public String getClave() {
		return clave;
	}

	public String getValor() {
		return valor;
	}

	public void setValor(String valor) {
		this.valor = valor;
	}
}
