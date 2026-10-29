package ar.gov.justiciajujuy.sanpedro.inventario.equipos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

	Optional<Equipo> findByNombreIgnoreCase(String nombre);

	long countByNombreIgnoreCaseNot(String nombre);

	List<Equipo> findAllByOrderByNombreAsc();

	List<Equipo> findByImpresoraRedAsignadaId(Long id);

	List<Equipo> findByIp(String ip);

	@Query("""
			SELECT e
			FROM Equipo e
			WHERE :query IS NULL
			   OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', :query, '%'))
			   OR LOWER(COALESCE(e.ultimoUsuario, '')) LIKE LOWER(CONCAT('%', :query, '%'))
			   OR LOWER(e.fuero) LIKE LOWER(CONCAT('%', :query, '%'))
			   OR LOWER(COALESCE(e.ubicacion, '')) LIKE LOWER(CONCAT('%', :query, '%'))
			   OR LOWER(COALESCE(e.ip, '')) LIKE LOWER(CONCAT('%', :query, '%'))
			   OR LOWER(COALESCE(e.mac, '')) LIKE LOWER(CONCAT('%', :query, '%'))
			   OR LOWER(COALESCE(e.motherboardSerial, '')) LIKE LOWER(CONCAT('%', :query, '%'))
			   OR LOWER(COALESCE(e.ramSeriales, '')) LIKE LOWER(CONCAT('%', :query, '%'))
			   OR LOWER(COALESCE(e.discosSeriales, '')) LIKE LOWER(CONCAT('%', :query, '%'))
			ORDER BY e.nombre
			""")
	Page<Equipo> buscar(@Param("query") String query, Pageable pageable);

	@Query("""
			SELECT e
			FROM Equipo e
			WHERE e.ultimoUsuario IS NOT NULL
			  AND TRIM(e.ultimoUsuario) <> ''
			  AND UPPER(e.nombre) <> 'PC-GENERICA'
			ORDER BY e.actualizadoEn DESC, e.id DESC
			""")
	List<Equipo> findAllConUltimoUsuarioValido();
}