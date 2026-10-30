package ar.gov.justiciajujuy.sanpedro.inventario.tareas;

/**
 * Origen del trabajo tecnico. Distingue lo que pide un empleado (incidente/solicitud)
 * del trabajo propio del taller (alistamiento/preventivo), que no tiene solicitante.
 */
public enum TipoTareaTecnica {

	INCIDENTE("Incidente / Solicitud de servicio", true),
	ALISTAMIENTO("Armado de PC en Taller", false);

	public static final String SOLICITANTE_TALLER = "Taller de Informática";
	public static final String SOLICITANTE_SISTEMAS_USERNAME = "sistemas";
	public static final String SOLICITANTE_SISTEMAS_NOMBRE = "Departamento de Sistemas (Uso Interno)";
	public static final String SOLICITANTE_SISTEMAS_FUERO = "Sistemas - Infraestructura y Taller";

	public static boolean esSolicitanteInternoSistemas(String valor) {
		if (valor == null) {
			return false;
		}
		String limpio = valor.trim().toLowerCase(java.util.Locale.ROOT);
		return "sistemas".equals(limpio)
				|| "taller".equals(limpio)
				|| "taller.sistemas".equals(limpio)
				|| "uso.interno".equals(limpio)
				|| "departamento de sistemas".equals(limpio)
				|| SOLICITANTE_SISTEMAS_NOMBRE.toLowerCase(java.util.Locale.ROOT).equals(limpio)
				|| SOLICITANTE_TALLER.toLowerCase(java.util.Locale.ROOT).equals(limpio);
	}

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
