package ar.gov.justiciajujuy.sanpedro.inventario.reportes;

import java.util.Arrays;
import java.util.List;

import ar.gov.justiciajujuy.sanpedro.inventario.equipos.EquipoRepository;
import ar.gov.justiciajujuy.sanpedro.inventario.muebles.MuebleService;
import ar.gov.justiciajujuy.sanpedro.inventario.muebles.MuebleService.MuebleDetalle;
import ar.gov.justiciajujuy.sanpedro.inventario.patrimonio.PatrimonioService;
import ar.gov.justiciajujuy.sanpedro.inventario.patrimonio.PatrimonioService.BienPatrimonialDetalle;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService.TareaTecnicaDetalle;
import org.springframework.stereotype.Service;

@Service
public class ReporteService {

	private final EquipoRepository equipoRepository;
	private final MuebleService muebleService;
	private final PatrimonioService patrimonioService;
	private final TareaTecnicaService tareaTecnicaService;

	public ReporteService(EquipoRepository equipoRepository, MuebleService muebleService,
			PatrimonioService patrimonioService, TareaTecnicaService tareaTecnicaService) {
		this.equipoRepository = equipoRepository;
		this.muebleService = muebleService;
		this.patrimonioService = patrimonioService;
		this.tareaTecnicaService = tareaTecnicaService;
	}

	public ResumenOperativo resumen() {
		return new ResumenOperativo(
				equipoRepository.count(),
				muebleService.contar(),
				patrimonioService.contar(),
				tareaTecnicaService.contar());
	}

	public String mueblesCsv() {
		StringBuilder csv = new StringBuilder("codigo,tipo,descripcion,ubicacion,fuero,responsable,estado,activo\n");
		for (MuebleDetalle mueble : muebleService.buscar(null, null)) {
			csv.append(fila(Arrays.asList(
					mueble.codigo(),
					mueble.tipo(),
					mueble.descripcion(),
					mueble.ubicacion(),
					mueble.fuero(),
					mueble.responsable(),
					String.valueOf(mueble.estado()),
					String.valueOf(mueble.activo()))));
		}
		return csv.toString();
	}

	public String patrimonioCsv() {
		StringBuilder csv = new StringBuilder("numeroPatrimonial,categoria,descripcion,ubicacion,fuero,custodio,estado,equipoNombre,activo\n");
		for (BienPatrimonialDetalle bien : patrimonioService.buscar(null, null)) {
			csv.append(fila(Arrays.asList(
					bien.numeroPatrimonial(),
					bien.categoria(),
					bien.descripcion(),
					bien.ubicacion(),
					bien.fuero(),
					bien.custodio(),
					String.valueOf(bien.estado()),
					bien.equipoNombre(),
					String.valueOf(bien.activo()))));
		}
		return csv.toString();
	}

	public String tareasCsv() {
		StringBuilder csv = new StringBuilder("id,titulo,equipoNombre,estado,prioridad,responsable\n");
		for (TareaTecnicaDetalle tarea : tareaTecnicaService.buscar(null, null, null)) {
			csv.append(fila(Arrays.asList(
					String.valueOf(tarea.id()),
					tarea.titulo(),
					tarea.equipoNombre(),
					String.valueOf(tarea.estado()),
					String.valueOf(tarea.prioridad()),
					tarea.responsable())));
		}
		return csv.toString();
	}

	private String fila(List<String> valores) {
		return valores.stream()
				.map(this::csv)
				.reduce((a, b) -> a + "," + b)
				.orElse("") + "\n";
	}

	private String csv(String valor) {
		if (valor == null) {
			return "";
		}
		String limpio = valor.replace("\"", "\"\"");
		return limpio.contains(",") || limpio.contains("\"") || limpio.contains("\n") ? "\"" + limpio + "\"" : limpio;
	}

	public record ResumenOperativo(long equipos, long muebles, long bienesPatrimoniales, long tareas) {
	}
}
