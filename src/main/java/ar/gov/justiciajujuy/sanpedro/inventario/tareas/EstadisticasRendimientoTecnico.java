package ar.gov.justiciajujuy.sanpedro.inventario.tareas;

import java.util.List;

public record EstadisticasRendimientoTecnico(
		String rango,
		long totalTareas,
		long totalRealizadas,
		long totalPendientes,
		double porcentajeRealizadas,
		double porcentajePendientes,
		List<RendimientoTecnicoItem> tecnicos,
		RendimientoTecnicoItem lider,
		RendimientoTecnicoItem segundo,
		TelefonistaMetricas telefonista
) {
	public record RendimientoTecnicoItem(
			String username,
			String nombreVisible,
			long realizadas,
			double porcentajeDelTotal,
			double porcentajeDeRealizadas,
			String color
	) {}

	public record TelefonistaMetricas(
			long ingresadasPeriodo,
			long ingresadasHoy,
			long ingresadasTotal,
			String operadorNombre
	) {}
}

