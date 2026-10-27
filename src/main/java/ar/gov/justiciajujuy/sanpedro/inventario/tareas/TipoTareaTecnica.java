package ar.gov.justiciajujuy.sanpedro.inventario.tareas;

/**
 * Origen del trabajo tecnico. Distingue lo que pide un empleado (incidente/solicitud)
 * del trabajo propio del taller (alistamiento/preventivo), que no tiene solicitante.
 */
public enum TipoTareaTecnica {

	INCIDENTE("Incidente / Solicitud de servicio", true),
	ALISTAMIENTO("Armado de PC en Taller", false);

	public static final String SOLICITANTE_TALLER = "Taller de Informática";

	private final String etiqueta;
	private final boolean requiereSolicitante;

	TipoTareaTecnica(String etiqueta, boolean requiereSolicitante) {
		this.etiqueta = etiqueta;
		this.requiereSolicitante = requiereSolicitante;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

	public boolean requiereSolicitante() {
		return requiereSolicitante;
	}

	/** Trabajo interno del taller: no nace de un pedido de un empleado. */
	public boolean esTrabajoInterno() {
		return !requiereSolicitante;
	}
}
