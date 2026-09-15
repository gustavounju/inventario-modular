package ar.gov.justiciajujuy.sanpedro.inventario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaAvisoService;
import ar.gov.justiciajujuy.sanpedro.inventario.tareas.TareaTecnicaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest(properties = { "inventario.local-auth.enabled=true", "inventario.local-auth.password=ClaveLocal123!" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql({"/sql/limpiar-seguridad-modular-test.sql", "/sql/seguridad-modular-test.sql"})
class TareaMovilControllerTests {
    @Autowired MockMvc mvc;
    @Autowired TareaTecnicaService tareas;
    @Autowired TareaAvisoService avisos;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired DataSource dataSource;

    private static final String NUEVA = """
            {"titulo":"Revisar impresora", "solicitanteUsername":"mesa", "solicitanteNombre":"Mesa de entradas",
             "solicitanteFuero":"Oficina", "prioridad":"ALTA"}
            """;

    @Test void ingresoMovilConservaDestinoYErrores() throws Exception {
        mvc.perform(get("/movil/tareas")).andExpect(redirectedUrl("/movil/login"));
        mvc.perform(get("/movil/login")).andExpect(status().isOk()).andExpect(content().string(containsString("name=\"destino\"")));
        mvc.perform(post("/login").with(csrf()).param("destino", "movil").param("username", "admin.local").param("password", "ClaveLocal123!"))
                .andExpect(redirectedUrl("/movil/tareas"));
        mvc.perform(post("/login").with(csrf()).param("destino", "movil").param("username", "admin.local").param("password", "invalida"))
                .andExpect(redirectedUrl("/movil/login?error"));
        mvc.perform(post("/login").with(csrf()).param("destino", "https://externo.test").param("username", "admin.local").param("password", "ClaveLocal123!"))
                .andExpect(redirectedUrl("/admin"));
    }

    @Test void pantallaIndependienteYPermisos() throws Exception {
        mvc.perform(get("/movil/tareas").with(user("admin.local"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Nueva tarea")))
                .andExpect(content().string(containsString("id=\"solicitante-search\"")))
                .andExpect(content().string(containsString("Escriba apellido, nombre o usuario de AD")))
                .andExpect(content().string(not(containsString("app-sidebar"))));
        mvc.perform(get("/api/v1/movil/sesion").with(user("admin.local"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.username").value("admin.local"))
                .andExpect(jsonPath("$.puedeCrear").value(true))
                .andExpect(jsonPath("$.puedeEditar").value(true))
                .andExpect(jsonPath("$.administrador").value(false));
        mvc.perform(get("/api/v1/movil/avisos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/movil/apk")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/movil/apk").with(user("sin.permisos"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/movil/apk/info").with(user("sin.permisos"))).andExpect(status().isForbidden());
        jdbc.update("INSERT INTO usuarios (id, username, nombre_visible, fuero, origen, activo) VALUES (99, 'solo.autorizado', 'Solo Autorizado', 'Informatica', 'AD', TRUE)");
        mvc.perform(get("/api/v1/movil/apk/info").with(user("admin.local"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").isBoolean());
        mvc.perform(get("/api/v1/movil/apk/info").with(user("solo.autorizado"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").isBoolean());
        jdbc.update("INSERT INTO roles (id, codigo, nombre, descripcion, activo) VALUES (2, 'TECNICO', 'Tecnico', 'Acceso operativo a tareas tecnicas.', TRUE)");
        jdbc.update("INSERT INTO usuarios (id, username, nombre_visible, fuero, origen, activo) VALUES (100, 'tecnico.movil', 'Tecnico Movil', 'Informatica', 'AD', TRUE)");
        jdbc.update("INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (100, 2)");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V16__permisos_tecnico_tareas_movil.sql")).execute(dataSource);
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V19__permisos_tecnico_stock_movil.sql")).execute(dataSource);
        mvc.perform(get("/movil/tareas").with(user("tecnico.movil"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Nueva tarea")))
                .andExpect(content().string(containsString("/movil/stock")));
        mvc.perform(get("/api/v1/movil/sesion").with(user("tecnico.movil"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.puedeCrear").value(true))
                .andExpect(jsonPath("$.puedeEditar").value(true))
                .andExpect(jsonPath("$.puedeEditarStock").value(true));
        mvc.perform(get("/movil/stock").with(user("tecnico.movil"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Guardar en stock")))
                .andExpect(content().string(containsString("scan-barcode")));
        mvc.perform(get("/api/v1/movil/stock/sesion").with(user("tecnico.movil"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.puedeEditarStock").value(true));
        mvc.perform(post("/api/v1/stock/componentes").with(user("tecnico.movil")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tipo":"MONITOR","estado":"DISPONIBLE","descripcion":"Monitor ingresado por escaneo BC-001",
                         "serial":"BC-001","activo":true}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serial").value("BC-001"))
                .andExpect(jsonPath("$.ingresadoPor").value("tecnico.movil"));
        mvc.perform(get("/api/v1/movil/avisos").with(user("sin.permisos"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/movil/usuarios-dominio?q=me").with(user("sin.permisos"))).andExpect(status().isForbidden());
        mvc.perform(get("/movil/tareas").with(user("sin.permisos"))).andExpect(status().isForbidden());
        mvc.perform(get("/movil/stock").with(user("sin.permisos"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/movil/avisos?despuesDe=-1").with(user("admin.local"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/movil/usuarios-dominio?q=me").with(user("admin.local"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.consultaRealizada").value(true));
        mvc.perform(get("/api/v1/tareas-tecnicas/1/comentarios").with(user("admin.local"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comentario").value("Comentario inicial de seguimiento."));
        mvc.perform(post("/api/v1/tareas-tecnicas").with(user("admin.local")).contentType(MediaType.APPLICATION_JSON).content(NUEVA))
                .andExpect(status().isCreated());
    }

    @Test void creacionMovilPermiteSolicitanteDistintoDelTecnicoLogueadoYComentariosVisiblesEnVisor() throws Exception {
        jdbc.update("INSERT INTO roles (id, codigo, nombre, descripcion, activo) VALUES (20, 'TECNICO_MOVIL_TEST', 'Tecnico movil test', 'Opera tareas desde la APK.', TRUE)");
        jdbc.update("INSERT INTO usuarios (id, username, nombre_visible, fuero, origen, activo) VALUES (120, 'tecnico.apk', 'Tecnico APK', 'Informatica', 'AD', TRUE)");
        jdbc.update("INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (120, 20)");
        jdbc.update("INSERT INTO rol_modulo_permisos (rol_id, modulo_id, permiso_id) VALUES (20, 9, 1), (20, 9, 3)");
        String tarea = """
                {"titulo":"Impresora sin imprimir", "descripcion":"La doctora Perez no puede imprimir.",
                 "solicitanteUsername":"dperez", "solicitanteNombre":"Doctora Perez",
                 "solicitanteFuero":"Oficina de Gestion Judicial", "prioridad":"MEDIA"}
                """;

        String json = mvc.perform(post("/api/v1/tareas-tecnicas")
                        .with(user("tecnico.apk"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                .content(tarea))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.solicitanteUsername").value("dperez"))
                .andExpect(jsonPath("$.solicitanteNombre").value("Doctora Perez"))
                .andExpect(jsonPath("$.responsable").value(nullValue()))
                .andReturn().getResponse().getContentAsString();

        Number id = com.jayway.jsonpath.JsonPath.read(json, "$.id");

        mvc.perform(post("/api/v1/tareas-tecnicas/{id}/comentarios", id.longValue())
                        .with(user("tecnico.apk"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"comentario":"Comentario cargado desde APK movil."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creadoEn").isNotEmpty());

        mvc.perform(get("/admin/tareas/visor"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Doctora Perez")))
                .andExpect(content().string(containsString("Usuario AD: dperez")))
                .andExpect(content().string(containsString("Fuero solicitante: Oficina de Gestion Judicial")))
                .andExpect(content().string(containsString("Pendiente de tomar")))
                .andExpect(content().string(containsString("Comentario cargado desde APK movil.")));
    }

    @Test void tecnicoCreadorPuedeComentarAunqueLaTareaNoEsteTomada() throws Exception {
        jdbc.update("INSERT INTO roles (id, codigo, nombre, descripcion, activo) VALUES (21, 'TECNICO_CREADOR_TEST', 'Tecnico creador test', 'Opera tareas desde la APK.', TRUE)");
        jdbc.update("INSERT INTO usuarios (id, username, nombre_visible, fuero, origen, activo) VALUES (121, 'tecnico.creador', 'Tecnico Creador', 'Informatica', 'AD', TRUE)");
        jdbc.update("INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (121, 21)");
        jdbc.update("INSERT INTO rol_modulo_permisos (rol_id, modulo_id, permiso_id) VALUES (21, 9, 1), (21, 9, 3)");
        var tarea = tareas.crear(new TareaTecnicaService.GuardarTareaTecnicaCommand(
                1L, "Creada sin responsable", "Caso historico cargado desde APK",
                "Perez Yolando", "Perez Yolando", "Oficina de Gestion Judicial",
                null, null, "tecnico.creador"));

        mvc.perform(post("/api/v1/tareas-tecnicas/{id}/comentarios", tarea.id())
                        .with(user("tecnico.creador"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"comentario":"Comentario permitido para el tecnico creador."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creadoEn").isNotEmpty());

        mvc.perform(get("/admin/tareas/visor"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Perez Yolando")))
                .andExpect(content().string(containsString("Fuero solicitante: Oficina de Gestion Judicial")))
                .andExpect(content().string(containsString("Pendiente de tomar")))
                .andExpect(content().string(containsString("Comentario permitido para el tecnico creador.")));
    }

    @Test void telefonistaCreaTareasLibresYTecnicosRecibenAvisoParaTomarlas() throws Exception {
        jdbc.update("INSERT INTO roles (id, codigo, nombre, descripcion, activo) VALUES (22, 'TECNICO_AVISO_TEST', 'Tecnico aviso test', 'Recibe avisos y toma tareas.', TRUE)");
        jdbc.update("INSERT INTO roles (id, codigo, nombre, descripcion, activo) VALUES (23, 'TELEFONISTA', 'Telefonista', 'Publica tareas para tecnicos.', TRUE)");
        jdbc.update("INSERT INTO usuarios (id, username, nombre_visible, fuero, origen, activo) VALUES (122, 'tecnico.avisos', 'Tecnico Avisos', 'Informatica', 'AD', TRUE)");
        jdbc.update("INSERT INTO usuarios (id, username, nombre_visible, fuero, origen, activo) VALUES (123, 'telefonista.apk', 'Telefonista APK', 'Mesa de Ayuda', 'AD', TRUE)");
        jdbc.update("INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (122, 22), (123, 23)");
        jdbc.update("INSERT INTO rol_modulo_permisos (rol_id, modulo_id, permiso_id) VALUES (22, 9, 1), (22, 9, 3), (23, 9, 1), (23, 9, 5)");

        mvc.perform(get("/api/v1/movil/sesion").with(user("telefonista.apk"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.puedeCrear").value(true))
                .andExpect(jsonPath("$.puedeEditar").value(false))
                .andExpect(jsonPath("$.puedeOperarPropias").value(true))
                .andExpect(jsonPath("$.mostrarMisTareas").value(false))
                .andExpect(jsonPath("$.mostrarFinalizadas").value(false))
                .andExpect(jsonPath("$.administrador").value(false));

        String json = mvc.perform(post("/api/v1/tareas-tecnicas")
                        .with(user("telefonista.apk"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"No abre el sistema", "descripcion":"Llamado telefonico recibido.",
                                 "solicitanteUsername":"jperez", "solicitanteNombre":"Juan Perez",
                                 "solicitanteFuero":"Oficina de Gestion Judicial", "prioridad":"ALTA"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creadoPor").value("telefonista.apk"))
                .andExpect(jsonPath("$.responsable").value(nullValue()))
                .andReturn().getResponse().getContentAsString();

        Number id = com.jayway.jsonpath.JsonPath.read(json, "$.id");

        mvc.perform(post("/api/v1/tareas-tecnicas/{id}/tomar", id.longValue())
                        .with(user("telefonista.apk"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/movil/avisos?despuesDe=0").with(user("telefonista.apk")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avisos").isEmpty());

        mvc.perform(get("/api/v1/movil/avisos?despuesDe=0").with(user("tecnico.avisos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avisos.length()").value(1))
                .andExpect(jsonPath("$.avisos[0].titulo").value("No abre el sistema"))
                .andExpect(jsonPath("$.avisos[0].autor").value("telefonista.apk"));

        mvc.perform(get("/api/v1/movil/avisos?despuesDe=0").with(user("admin.local")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avisos.length()").value(1));

        mvc.perform(post("/api/v1/tareas-tecnicas/{id}/tomar", id.longValue())
                        .with(user("tecnico.avisos"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsable").value("tecnico.avisos"));
    }

    @Test void telefonistaEditaYBorraSoloAntesDeTomaYLosAvisosDirigidosNoSonGenerales() throws Exception {
        jdbc.update("INSERT INTO roles (id, codigo, nombre, descripcion, activo) VALUES (24, 'TECNICO', 'Tecnico', 'Recibe avisos dirigidos.', TRUE)");
        jdbc.update("INSERT INTO roles (id, codigo, nombre, descripcion, activo) VALUES (25, 'TELEFONISTA', 'Telefonista', 'Carga llamados.', TRUE)");
        jdbc.update("INSERT INTO usuarios (id, username, nombre_visible, fuero, origen, activo) VALUES (124, 'tecnico.dirigido', 'Tecnico Dirigido', 'Informatica', 'AD', TRUE)");
        jdbc.update("INSERT INTO usuarios (id, username, nombre_visible, fuero, origen, activo) VALUES (125, 'tecnico.otro', 'Tecnico Otro', 'Informatica', 'AD', TRUE)");
        jdbc.update("INSERT INTO usuarios (id, username, nombre_visible, fuero, origen, activo) VALUES (126, 'telefonista.dirigido', 'Telefonista Dirigido', 'Mesa de Ayuda', 'AD', TRUE)");
        jdbc.update("INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (124, 24), (125, 24), (126, 25)");
        jdbc.update("INSERT INTO rol_modulo_permisos (rol_id, modulo_id, permiso_id) VALUES (24, 9, 1), (24, 9, 3), (25, 9, 1), (25, 9, 5)");

        mvc.perform(get("/api/v1/movil/tecnicos-asignables?q=dir").with(user("telefonista.dirigido")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarios[0].username").value("tecnico.dirigido"));

        String libreJson = mvc.perform(post("/api/v1/tareas-tecnicas")
                        .with(user("telefonista.dirigido"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Libre editable", "descripcion":"Problema inicial.",
                                 "solicitanteUsername":"mlopez", "solicitanteNombre":"Maria Lopez",
                                 "solicitanteFuero":"Oficina", "prioridad":"MEDIA"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responsable").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        Number libreId = com.jayway.jsonpath.JsonPath.read(libreJson, "$.id");

        mvc.perform(put("/api/v1/tareas-tecnicas/{id}", libreId.longValue())
                        .with(user("telefonista.dirigido"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Libre editable", "descripcion":"Problema corregido por telefonista.",
                                 "solicitanteUsername":"mlopez", "solicitanteNombre":"Maria Lopez",
                                 "solicitanteFuero":"Oficina", "prioridad":"ALTA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Problema corregido por telefonista."));

        mvc.perform(delete("/api/v1/tareas-tecnicas/{id}", libreId.longValue())
                        .with(user("telefonista.dirigido"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
        Long cursorAvisosDirigidos = jdbc.queryForObject(
                "SELECT ultimo_id FROM tareas_aviso_secuencia WHERE id = 1", Long.class);

        String asignadaJson = mvc.perform(post("/api/v1/tareas-tecnicas")
                        .with(user("telefonista.dirigido"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Asignada directa", "descripcion":"Aviso dirigido.",
                                 "solicitanteUsername":"jlopez", "solicitanteNombre":"Jose Lopez",
                                 "solicitanteFuero":"Oficina", "prioridad":"ALTA", "responsable":"tecnico.dirigido"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responsable").value("tecnico.dirigido"))
                .andReturn().getResponse().getContentAsString();
        Number asignadaId = com.jayway.jsonpath.JsonPath.read(asignadaJson, "$.id");

        mvc.perform(delete("/api/v1/tareas-tecnicas/{id}", asignadaId.longValue())
                        .with(user("telefonista.dirigido"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/tareas-tecnicas/{id}/comentarios", asignadaId.longValue())
                        .with(user("telefonista.dirigido"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comentario\":\"Amplio datos del llamado.\"}"))
                .andExpect(status().isCreated());

        tareas.cambiarEstado(asignadaId.longValue(), new TareaTecnicaService.CambiarEstadoTareaCommand(
                ar.gov.justiciajujuy.sanpedro.inventario.tareas.EstadoTareaTecnica.CERRADA, "Resuelta"));
        mvc.perform(get("/api/v1/tareas-tecnicas").with(user("telefonista.dirigido")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + asignadaId.longValue() + ")]").isEmpty());

        mvc.perform(get("/api/v1/movil/avisos?despuesDe=" + cursorAvisosDirigidos).with(user("tecnico.dirigido")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avisos.length()").value(3))
                .andExpect(jsonPath("$.avisos[0].destinatarioUsername").value("tecnico.dirigido"));
        mvc.perform(get("/api/v1/movil/avisos?despuesDe=" + cursorAvisosDirigidos).with(user("tecnico.otro")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avisos.length()").value(0));
        mvc.perform(get("/api/v1/movil/avisos?despuesDe=" + cursorAvisosDirigidos).with(user("admin.local")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avisos.length()").value(0));
    }

    @Test void creacionDesdeApiGeneraAvisoRecuperableSinDuplicar() throws Exception {
        mvc.perform(get("/api/v1/movil/avisos").with(user("admin.local")))
                .andExpect(jsonPath("$.siguiente").value(0)).andExpect(jsonPath("$.avisos").isEmpty());
        mvc.perform(post("/api/v1/tareas-tecnicas").with(user("admin.local")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(NUEVA))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/movil/avisos?despuesDe=0").with(user("admin.local")))
                .andExpect(jsonPath("$.avisos.length()").value(1)).andExpect(jsonPath("$.avisos[0].titulo").value("Revisar impresora"))
                .andExpect(jsonPath("$.avisos[0].autor").value("admin.local")).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/v1/movil/avisos?despuesDe=1").with(user("admin.local"))).andExpect(jsonPath("$.avisos").isEmpty());
        // Inicializar un telefono nuevo no hace sonar todas las tareas anteriores.
        mvc.perform(get("/api/v1/movil/avisos").with(user("admin.local"))).andExpect(jsonPath("$.siguiente").value(1)).andExpect(jsonPath("$.avisos").isEmpty());
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V15__avisos_tareas_lan.sql")).execute(dataSource);
        assertThat(avisos.consultar(0L, "admin.local").avisos()).hasSize(1);
    }

    @Test void rollbackDeTareaNoDejaAvisosFantasma() {
        long total = tareas.contar();
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            tareas.crear(new TareaTecnicaService.GuardarTareaTecnicaCommand(1L, "No confirmar", null, "mesa", "Mesa", "Oficina", null, null, "admin.local"));
            tx.setRollbackOnly();
        });
        assertThat(tareas.contar()).isEqualTo(total);
        assertThat(avisos.consultar(0L, "admin.local").avisos()).isEmpty();
        assertThat(avisos.consultar(null, "admin.local").siguiente()).isZero();
    }

    @Test void paginaAvisosEnOrdenYRecuperaCursorDeBaseRestaurada() {
        for (long i = 1; i <= 105; i++) {
            jdbc.update("INSERT INTO tareas_avisos (id,tarea_id,titulo,autor) VALUES (?,?,?,?)", i, 1L, "Tarea", "admin.local");
        }
        jdbc.update("UPDATE tareas_aviso_secuencia SET ultimo_id = 105 WHERE id = 1");
        var primera = avisos.consultar(0L, "admin.local");
        assertThat(primera.avisos()).hasSize(100);
        assertThat(primera.siguiente()).isEqualTo(100);
        assertThat(avisos.consultar(primera.siguiente(), "admin.local").avisos()).hasSize(5);
        assertThat(avisos.consultar(900L, "admin.local").siguiente()).isEqualTo(105);
    }

    @Test void comentariosGeneranAvisosBroadcastODirigidos() throws Exception {
        var libre = tareas.crear(new TareaTecnicaService.GuardarTareaTecnicaCommand(1L, "Libre", null, "mesa", "Mesa", "Oficina", null, null, "admin.local"));
        tareas.comentar(libre.id(), new TareaTecnicaService.AgregarComentarioTareaCommand("admin.local", "Comentario general"));
        var broadcast = avisos.consultar(0L, "tecnico.uno").avisos();
        assertThat(broadcast).anySatisfy(aviso -> {
            assertThat(aviso.tareaId()).isEqualTo(libre.id());
            assertThat(aviso.tipo()).isEqualTo("COMENTARIO");
            assertThat(aviso.destinatarioUsername()).isNull();
        });

        var tomada = tareas.crear(new TareaTecnicaService.GuardarTareaTecnicaCommand(1L, "Tomada", null, "mesa", "Mesa", "Oficina", null, "tecnico.uno", "admin.local"));
        tareas.comentar(tomada.id(), new TareaTecnicaService.AgregarComentarioTareaCommand("admin.local", "Comentario dirigido"));
        assertThat(avisos.consultar(0L, "tecnico.uno").avisos()).anySatisfy(aviso -> {
            assertThat(aviso.tareaId()).isEqualTo(tomada.id());
            assertThat(aviso.tipo()).isEqualTo("COMENTARIO");
            assertThat(aviso.destinatarioUsername()).isEqualTo("tecnico.uno");
        });
        assertThat(avisos.consultar(0L, "tecnico.dos").avisos())
                .noneMatch(aviso -> aviso.tareaId() == tomada.id() && "COMENTARIO".equals(aviso.tipo()));
    }

    @Test void dosTecnicosNoPuedenTomarLaMismaTarea() throws Exception {
        var creada = tareas.crear(new TareaTecnicaService.GuardarTareaTecnicaCommand(1L, "Concurrente", null, "mesa", "Mesa", "Oficina", null, null, "admin.local"));
        CountDownLatch inicio = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var uno = workers.submit(() -> tomarAlMismoTiempo(inicio, creada.id(), "tecnico.uno"));
            var dos = workers.submit(() -> tomarAlMismoTiempo(inicio, creada.id(), "tecnico.dos"));
            inicio.countDown();
            assertThat(uno.get(10, TimeUnit.SECONDS) + dos.get(10, TimeUnit.SECONDS)).isEqualTo(1);
        }
    }

    @Test void tareaFinalizadaNoPuedeVolverATomarse() throws Exception {
        tareas.cambiarEstado(1L, new TareaTecnicaService.CambiarEstadoTareaCommand(ar.gov.justiciajujuy.sanpedro.inventario.tareas.EstadoTareaTecnica.CERRADA, "Resuelta"));
        mvc.perform(post("/api/v1/tareas-tecnicas/1/tomar").with(user("admin.local")).with(csrf())).andExpect(status().isConflict());
    }

    private int tomarAlMismoTiempo(CountDownLatch inicio, Long id, String username) throws Exception {
        inicio.await(5, TimeUnit.SECONDS);
        try { tareas.tomar(id, username); return 1; }
        catch (TareaTecnicaService.TareaTecnicaYaAsignadaException expected) { return 0; }
    }
}
