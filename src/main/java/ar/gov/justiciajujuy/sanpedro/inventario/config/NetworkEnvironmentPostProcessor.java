package ar.gov.justiciajujuy.sanpedro.inventario.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

public class NetworkEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Log log = LogFactory.getLog(NetworkEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Detect if we are already forcing a profile
        if (Arrays.asList(environment.getActiveProfiles()).contains("casa") ||
            Arrays.asList(environment.getActiveProfiles()).contains("trabajo") ||
            Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            return;
        }

        // Host and port to check for the remote database
        String remoteDbHost = "10.15.0.62";
        int remoteDbPort = 3306;

        log.info("Verificando conexion a la base de datos remota (" + remoteDbHost + ":" + remoteDbPort + ") para determinar el entorno...");

        boolean isReachable = false;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(remoteDbHost, remoteDbPort), 1500);
            isReachable = true;
        } catch (IOException e) {
            // Ignored
        }

        if (isReachable) {
            log.info("Base de datos remota alcanzable. Mantenemos perfil predeterminado (trabajo/local).");
        } else {
            log.info("Base de datos remota INALCANZABLE. Activando perfil 'casa' como fallback automatico y usando DB MySQL local.");
            environment.addActiveProfile("casa");
        }
    }
}
