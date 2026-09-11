package ar.gov.justiciajujuy.sanpedro.inventario.tareas;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TareaAvisoService {
    private final JdbcTemplate jdbc;

    public TareaAvisoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // JDBC comparte datasource y transaccion con JPA; no publicar desde una transaccion independiente.
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarCreacion(TareaTecnica tarea) {
        registrar(tarea, "CREACION", tarea.getCreadoPor(), null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarComentario(TareaTecnica tarea, String autor) {
        registrar(tarea, "COMENTARIO", autor, tarea.getResponsable());
    }

    private void registrar(TareaTecnica tarea, String tipo, String autor, String destinatario) {
        // El bloqueo se conserva hasta el commit de la tarea: ningun cursor salta avisos sin confirmar.
        Long ultimo = jdbc.queryForObject(
                "SELECT ultimo_id FROM tareas_aviso_secuencia WHERE id = 1 FOR UPDATE", Long.class);
        long siguiente = ultimo + 1;
        jdbc.update("UPDATE tareas_aviso_secuencia SET ultimo_id = ? WHERE id = 1", siguiente);
        jdbc.update("""
                INSERT INTO tareas_avisos (id, tarea_id, titulo, autor, tipo, destinatario_username)
                VALUES (?, ?, ?, ?, ?, ?)
                """, siguiente, tarea.getId(), tarea.getTitulo(), autor, tipo, destinatario);
    }

    @Transactional(readOnly = true)
    public LoteAvisos consultar(Long despuesDe, String username) {
        long actual = jdbc.queryForObject("SELECT ultimo_id FROM tareas_aviso_secuencia WHERE id = 1", Long.class);
        // Primer ingreso o base restaurada: establecer referencia, sin hacer sonar todo el historial.
        if (despuesDe == null || despuesDe > actual) {
            return new LoteAvisos(actual, List.of());
        }
        List<Aviso> avisos = jdbc.query(
                """
                SELECT id, tarea_id, titulo, autor, tipo, destinatario_username, creado_en
                FROM tareas_avisos
                WHERE id > ?
                  AND (destinatario_username IS NULL OR LOWER(destinatario_username) = LOWER(?))
                ORDER BY id
                LIMIT 100
                """,
                (rs, row) -> new Aviso(rs.getLong("id"), rs.getLong("tarea_id"), rs.getString("titulo"),
                        rs.getString("autor"), rs.getString("tipo"), rs.getString("destinatario_username"),
                        rs.getTimestamp("creado_en").toLocalDateTime()), despuesDe, username);
        // Avanzar solo hasta el lote entregado; usar 'actual' perderia los avisos de la pagina siguiente.
        long siguiente = avisos.isEmpty() ? despuesDe : avisos.getLast().id();
        return new LoteAvisos(siguiente, avisos);
    }

    public record Aviso(long id, long tareaId, String titulo, String autor, String tipo, String destinatarioUsername,
            LocalDateTime creadoEn) { }
    public record LoteAvisos(long siguiente, List<Aviso> avisos) { }
}
