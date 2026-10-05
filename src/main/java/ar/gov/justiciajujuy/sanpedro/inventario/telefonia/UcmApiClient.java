package ar.gov.justiciajujuy.sanpedro.inventario.telefonia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.LocalDate;

@Service
public class UcmApiClient {

    private static final Logger log = LoggerFactory.getLogger(UcmApiClient.class);
    private final ObjectMapper objectMapper;
    private final SSLContext sslContext;
    
    @Value("${ucm.url:https://10.15.0.2:8089/api}")
    private String baseUrl;
    
    @Value("${ucm.user:apiuser}")
    private String apiUser;
    
    @Value("${ucm.password:}")
    private String apiPassword;
    
    @Value("${ucm.interno:1005}")
    private String internoTaller;

    public UcmApiClient() {
        this.sslContext = crearSslContextInseguro();
        this.objectMapper = new ObjectMapper();
    }

    private SSLContext crearSslContextInseguro() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustAllCerts, new SecureRandom());
            return context;
        } catch (Exception e) {
            throw new RuntimeException("Error creando SSLContext: " + e.getMessage(), e);
        }
    }

    public String obtenerHistorialCdr() {
        try {
            if (apiPassword == null || apiPassword.isBlank()) {
                return "{\"error\":\"Falta configurar ucm.password para consultar la central IP.\"}";
            }

            // 1. Obtener Challenge
            String challenge = realizarPeticion("{\"request\":\"challenge\",\"user\":\"" + apiUser + "\"}");
            JsonNode challengeNode = objectMapper.readTree(challenge);
            if (challengeNode.get("status") == null || challengeNode.get("status").asInt() != 0) {
                log.warn("Fallo al obtener challenge de UCM. Respuesta: {}", challenge);
                int status = challengeNode.path("status").asInt(-99);
                if (status == -1) {
                    return "{\"error\":\"La central rechazó la conexión (status: -1). Verifique que en la pestaña 'Configuración de la API (nueva)' esté activada la API y creado el usuario " + apiUser + " con la IP permitida.\"}";
                }
                return "{\"error\":\"Fallo al obtener challenge de la central (status: " + status + ")\"}";
            }
            String challengeStr = challengeNode.path("response").path("challenge").asText();

            // 2. Generar MD5 Token (challenge + password)
            String token = generarMd5(challengeStr + apiPassword);

            // 3. Login
            String login = realizarPeticion("{\"request\":\"login\",\"user\":\"" + apiUser + "\",\"token\":\"" + token + "\"}");
            JsonNode loginNode = objectMapper.readTree(login);
            if (loginNode.get("status") == null || loginNode.get("status").asInt() != 0) {
                log.warn("Fallo login en central UCM. Respuesta: {}", login);
                return "{\"error\":\"Fallo de autenticación en la central. Verifique que la contraseña sea correcta.\"}";
            }
            String cookie = loginNode.path("response").path("cookie").asText();

            // 4. Pedir CDR del día de hoy filtrado por interno (callee)
            String hoy = LocalDate.now().toString();
            String cdrPayload = String.format(
                "{\"request\":\"getCDR\",\"cookie\":\"%s\",\"starttime\":\"%s 00:00:00\",\"endtime\":\"%s 23:59:59\",\"callee\":\"%s\"}",
                cookie, hoy, hoy, internoTaller
            );
            return realizarPeticion(cdrPayload);
            
        } catch (Exception e) {
            log.error("Error al obtener CDR de UCM", e);
            return String.format("{\"error\": \"Error al conectar con la central IP: %s\"}", e.getMessage());
        }
    }

    private String realizarPeticion(String jsonPayload) throws Exception {
        URL url = new URL(baseUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setSSLSocketFactory(this.sslContext.getSocketFactory());
        // Desactivar validación de Hostname
        conn.setHostnameVerifier((hostname, session) -> true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode >= 400) {
            String errorMsg = "";
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line.trim());
                errorMsg = sb.toString();
            } catch (Exception ignored) {}
            log.error("Error HTTP {} de central UCM: {}", responseCode, errorMsg);
            throw new RuntimeException("HTTP " + responseCode + (errorMsg.isEmpty() ? "" : " - " + errorMsg));
        }
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }

    private String generarMd5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(input.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
