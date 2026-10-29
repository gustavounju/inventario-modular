package ar.gov.justiciajujuy.sanpedro.inventario.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NetworkEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Log log = LogFactory.getLog(NetworkEnvironmentPostProcessor.class);
    private static final String DEFAULT_JUDICIAL_PROBE_HOST = "10.15.0.41";
    private static final int DEFAULT_JUDICIAL_PROBE_PORT = 389;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.contains("test")
                || activeProfiles.contains("casa")
                || activeProfiles.contains("trabajo")
                || activeProfiles.contains("prod")
                || activeProfiles.contains("local")
                || isRunningUnderTest()) {
            return;
        }

        // Host and port to check for judicial network connectivity (defaults to domain controller 10.15.0.41:389)
        String autoDetect = environment.getProperty("INVENTARIO_AUTO_DETECT_NETWORK", "true");
        if ("false".equalsIgnoreCase(autoDetect)) {
            return;
        }

        String probeHost = environment.getProperty("INVENTARIO_PROBE_HOST");
        if (!StringUtils.hasText(probeHost)) {
            probeHost = environment.getProperty("INVENTARIO_PROBE_DB_HOST");
        }
        if (!StringUtils.hasText(probeHost)) {
            probeHost = DEFAULT_JUDICIAL_PROBE_HOST;
        }

        int probePort = DEFAULT_JUDICIAL_PROBE_PORT;
        String probePortProp = environment.getProperty("INVENTARIO_PROBE_PORT");
        if (!StringUtils.hasText(probePortProp)) {
            probePortProp = environment.getProperty("INVENTARIO_PROBE_DB_PORT");
        }
        if (StringUtils.hasText(probePortProp)) {
            try {
                probePort = Integer.parseInt(probePortProp.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        log.info("Verificando conexion a la Red Judicial (" + probeHost + ":" + probePort + ") para determinar el entorno (Casa vs Produccion)...");

        boolean isReachable = false;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(probeHost, probePort), 1200);
            isReachable = true;
        } catch (IOException e) {
            // Ignored
        }

        if (isReachable) {
            log.info("Red Judicial detectada (" + probeHost + ":" + probePort + " alcanzable). Operando en Modo Produccion/Trabajo.");
        } else {
            log.info("Red Judicial (" + probeHost + ":" + probePort + ") INALCANZABLE. Activando Modo Casa automatico (DB MySQL local 127.0.0.1, AD y UCM pasivos).");
            environment.addActiveProfile("casa");
            Map<String, Object> overrides = new LinkedHashMap<>();
            if (!StringUtils.hasText(environment.getProperty("INVENTARIO_LDAP_ENABLED"))) {
                overrides.put("inventario.ldap.enabled", "false");
            }
            if (!StringUtils.hasText(environment.getProperty("UCM_ENABLED"))) {
                overrides.put("ucm.enabled", "false");
            }
            if (!overrides.isEmpty()) {
                environment.getPropertySources().addFirst(new MapPropertySource("modoCasaAutoOverrides", overrides));
            }
        }
    }

    private boolean isRunningUnderTest() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String className = element.getClassName();
            if (className.startsWith("org.junit.") || className.startsWith("org.springframework.boot.test.")) {
                return true;
            }
        }
        return false;
    }
}
