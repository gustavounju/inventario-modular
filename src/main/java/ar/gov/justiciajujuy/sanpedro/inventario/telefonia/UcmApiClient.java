package ar.gov.justiciajujuy.sanpedro.inventario.telefonia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDate;

@Service
public class UcmApiClient {

    private static final Logger log = LoggerFactory.getLogger(UcmApiClient.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    @Value("${ucm.url:https://10.15.0.2:8089/api}")
    private String baseUrl;
    
    @Value("${ucm.user:apiuser}")
    private String apiUser;
    
    @Value("${ucm.password:}")
    private String apiPassword;
    
    @Value("${ucm.interno:1005}")
    private String internoTaller;

    public UcmApiClient() {
        // Desactivar la verificación de Hostname (Subject Alternative Name) para el HttpClient de Java 11+
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
        this.httpClient = crearHttpClientInseguro();
        this.objectMapper = new ObjectMapper();
    }

    private HttpClient crearHttpClientInseguro() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error creando HttpClient SSL: " + e.getMessage(), e);
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
            if (challengeNode.get("status").asInt() != 0) return "{\"error\":\"Fallo al obtener challenge\"}";
            String challengeStr = challengeNode.path("response").path("challenge").asText();

            // 2. Generar MD5 Token (challenge + password)
            String token = generarMd5(challengeStr + apiPassword);

            // 3. Login
            String login = realizarPeticion("{\"request\":\"login\",\"user\":\"" + apiUser + "\",\"token\":\"" + token + "\"}");
            JsonNode loginNode = objectMapper.readTree(login);
            if (loginNode.get("status").asInt() != 0) return "{\"error\":\"Fallo login en central\"}";
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
            return "{\"error\": \"No se pudo conectar a la central IP o error procesando datos.\"}";
        }
    }

    private String realizarPeticion(String jsonPayload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(baseUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
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
