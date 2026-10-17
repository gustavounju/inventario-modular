package ar.gov.justiciajujuy.sanpedro.inventario.red;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RedIpRepository extends JpaRepository<RedIp, Long> {

    List<RedIp> findAllByOrderByIpNumeroAsc();

    Optional<RedIp> findByIp(String ip);

    Optional<RedIp> findByIpNumero(Integer ipNumero);

    boolean existsByIp(String ip);

    List<RedIp> findByEstadoOrderByIpNumeroAsc(EstadoIp estado);

    Optional<RedIp> findFirstByEstadoOrderByIpNumeroAsc(EstadoIp estado);

    long countByEstado(EstadoIp estado);

    long countByEstadoNot(EstadoIp estado);

    @Query("""
        SELECT r FROM RedIp r
        WHERE LOWER(r.ip) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(COALESCE(r.equipo, '')) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(COALESCE(r.detalle, '')) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(COALESCE(r.observaciones, '')) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY r.ipNumero ASC
    """)
    List<RedIp> buscarPorTexto(@Param("query") String query);
}
