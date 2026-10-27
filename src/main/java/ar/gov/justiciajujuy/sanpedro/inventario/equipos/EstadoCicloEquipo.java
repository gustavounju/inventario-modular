package ar.gov.justiciajujuy.sanpedro.inventario.equipos;

/**
 * Ciclo de vida ITAM del activo. Independiente de {@code activo} (baja logica) y de
 * {@code monitoreo} (telemetria del script).
 */
public enum EstadoCicloEquipo {

	EN_SERVICIO("En servicio"),
	EN_ARMADO("En armado en taller"),
	DISPONIBLE("Disponible en taller"),
	EN_REPARACION("En reparación"),
	BAJA("Baja");

	private final String etiqueta;

	EstadoCicloEquipo(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	public String getEtiqueta() {
		return etiqueta;
	}
}
