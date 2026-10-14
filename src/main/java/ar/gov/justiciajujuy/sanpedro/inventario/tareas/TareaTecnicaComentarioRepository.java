package ar.gov.justiciajujuy.sanpedro.inventario.tareas;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TareaTecnicaComentarioRepository extends JpaRepository<TareaTecnicaComentario, Long> {

	List<TareaTecnicaComentario> findByTareaIdOrderByCreadoEnDescIdDesc(Long tareaId);

	@Query("""
			SELECT c
			FROM TareaTecnicaComentario c
			JOIN FETCH c.tarea t
			WHERE t.id IN :tareaIds
			ORDER BY c.creadoEn DESC, c.id DESC
			""")
	List<TareaTecnicaComentario> buscarPorTareas(@Param("tareaIds") Collection<Long> tareaIds);

	void deleteByTareaId(Long tareaId);
}
