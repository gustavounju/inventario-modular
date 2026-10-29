package ar.gov.justiciajujuy.sanpedro.inventario.tareas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import ar.gov.justiciajujuy.sanpedro.inventario.auditoria.AuditoriaService;
import ar.gov.justiciajujuy.sanpedro.inventario.equipos.Equipo;
import ar.gov.justiciajujuy.sanpedro.inventario.equipos.EquipoRepository;
import org.junit.jupiter.api.Test;

class TareaConciliacionServiceTests {

	private Equipo crearEquipo(String nombre, String fuero, String ultimoUsuario) {
		Equipo e = new Equipo(nombre, fuero);
		e.setUltimoUsuario(ultimoUsuario);
		return e;
	}

	@Test
	void analizaYVinculaCuandoHayCoincidenciaExactaUnoAUno() {
		TareaTecnicaRepository tareaRepository = mock(TareaTecnicaRepository.class);
		EquipoRepository equipoRepository = mock(EquipoRepository.class);
		AuditoriaService auditoriaService = mock(AuditoriaService.class);

		TareaTecnica tarea = new TareaTecnica("Falla impresora");
		tarea.actualizarDatos(null, "Falla impresora", "Detalle", "mtomas", "Marianela Tomas",
				"Oficina de Gestion Judicial", PrioridadTareaTecnica.MEDIA, null);

		Equipo equipo = crearEquipo("CCYCSIV1200007", "Oficina de Gestion Judicial", "mtomas");

		when(tareaRepository.buscarTareasConEquipoGenerico()).thenReturn(List.of(tarea));
		when(equipoRepository.findAllConUltimoUsuarioValido()).thenReturn(List.of(equipo));

		TareaConciliacionService service = new TareaConciliacionService(tareaRepository, equipoRepository, auditoriaService);
		TareaConciliacionService.ResumenConciliacion resumen = service.analizar();

		assertThat(resumen.totalGenericas()).isEqualTo(1);
		assertThat(resumen.totalVinculables()).isEqualTo(1);
		assertThat(resumen.totalSinCoincidencia()).isEqualTo(0);
		assertThat(resumen.propuestas()).hasSize(1);
		assertThat(resumen.propuestas().get(0).equipoSugeridoNombre()).isEqualTo("CCYCSIV1200007");
		assertThat(resumen.propuestas().get(0).criterioMatch()).contains("1 a 1");
	}

	@Test
	void desempataPorFueroCuandoUsuarioTieneMultiplesEquipos() {
		TareaTecnicaRepository tareaRepository = mock(TareaTecnicaRepository.class);
		EquipoRepository equipoRepository = mock(EquipoRepository.class);
		AuditoriaService auditoriaService = mock(AuditoriaService.class);

		TareaTecnica tarea = new TareaTecnica("Configurar correo");
		tarea.actualizarDatos(null, "Configurar correo", "", "dquiros", "Daniel Quiros",
				"Informatica", PrioridadTareaTecnica.MEDIA, null);

		Equipo pcJuzgado = crearEquipo("PC-JUZG-01", "Juzgado Civil", "dquiros");
		Equipo pcInformatica = crearEquipo("PC-INF-05", "Informatica", "dquiros");

		when(tareaRepository.buscarTareasConEquipoGenerico()).thenReturn(List.of(tarea));
		when(equipoRepository.findAllConUltimoUsuarioValido()).thenReturn(List.of(pcJuzgado, pcInformatica));

		TareaConciliacionService service = new TareaConciliacionService(tareaRepository, equipoRepository, auditoriaService);
		TareaConciliacionService.ResumenConciliacion resumen = service.analizar();

		assertThat(resumen.totalVinculables()).isEqualTo(1);
		assertThat(resumen.propuestas().get(0).equipoSugeridoNombre()).isEqualTo("PC-INF-05");
		assertThat(resumen.propuestas().get(0).criterioMatch()).contains("Fuero");
	}

	@Test
	void normalizaUsuariosConDominioPrefix() {
		assertThat(TareaConciliacionService.normalizar("PODJUDSP\\mtomas")).isEqualTo("mtomas");
		assertThat(TareaConciliacionService.normalizar("mtomas@podjudsp.local")).isEqualTo("mtomas");
		assertThat(TareaConciliacionService.normalizar("  MTOMAS  ")).isEqualTo("mtomas");
	}
}
