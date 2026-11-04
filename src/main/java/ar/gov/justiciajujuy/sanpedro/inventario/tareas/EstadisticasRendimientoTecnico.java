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
		TelefonistaMetricas telefonista,
		PendientesMetricas pendientesDetalle
) {
	public EstadisticasRendimientoTecnico(
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
		this(rango, totalTareas, totalRealizadas, totalPendientes, porcentajeRealizadas, porcentajePendientes, tecnicos, lider, segundo, telefonista, null);
	}

	public record DistribucionItem(
			String etiqueta,
			long cantidad,
			double porcentaje,
			String color
	) {}

	public record RendimientoTecnicoItem(
			String username,
			String nombreVisible,
			long realizadas,
			double porcentajeDelTotal,
			double porcentajeDeRealizadas,
			String color,
			List<DistribucionItem> fueros,
			List<DistribucionItem> usuarios,
			List<DistribucionItem> tipos
	) {
		public RendimientoTecnicoItem(
				String username,
				String nombreVisible,
				long realizadas,
				double porcentajeDelTotal,
				double porcentajeDeRealizadas,
				String color,
				List<DistribucionItem> fueros,
				List<DistribucionItem> usuarios
		) {
			this(username, nombreVisible, realizadas, porcentajeDelTotal, porcentajeDeRealizadas, color, fueros, usuarios, List.of());
		}

		public RendimientoTecnicoItem(
				String username,
				String nombreVisible,
				long realizadas,
				double porcentajeDelTotal,
				double porcentajeDeRealizadas,
				String color
		) {
			this(username, nombreVisible, realizadas, porcentajeDelTotal, porcentajeDeRealizadas, color, List.of(), List.of(), List.of());
		}
	}

	public record TelefonistaMetricas(
			long ingresadasPeriodo,
			long ingresadasHoy,
			long ingresadasTotal,
			String operadorNombre
	) {}

	public record PendientesMetricas(
			long total,
			double porcentaje,
			List<DistribucionItem> fueros,
			List<DistribucionItem> usuarios,
			List<DistribucionItem> tipos,
			long sinAsignar,
			long enProceso
	) {
		public PendientesMetricas(
				long total,
				double porcentaje,
				List<DistribucionItem> fueros,
				List<DistribucionItem> usuarios,
				List<DistribucionItem> tipos
		) {
			this(total, porcentaje, fueros, usuarios, tipos, 0, 0);
		}

		public PendientesMetricas(
				long total,
				double porcentaje,
				List<DistribucionItem> fueros,
				List<DistribucionItem> usuarios
		) {
			this(total, porcentaje, fueros, usuarios, List.of(), 0, 0);
		}
	}
}


