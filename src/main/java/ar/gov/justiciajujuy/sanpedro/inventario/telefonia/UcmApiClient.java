package ar.gov.justiciajujuy.sanpedro.inventario.telefonia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    private volatile String trackId = null;
    
    @Value("${ucm.url:https://10.15.0.2:8089/api}")
    private String baseUrl;
    
    @Value("${ucm.user:cdrapi}")
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

            // Iniciar sesión limpia
            this.trackId = null;

            // 1. Obtener Challenge (Estructura oficial Grandstream: request -> action)
            String challengePayload = String.format("{\"request\":{\"action\":\"challenge\",\"user\":\"%s\",\"version\":\"1.0\"}}", apiUser);
            String challengeRes = realizarPeticion(challengePayload);
            JsonNode challengeNode = objectMapper.readTree(challengeRes);
            if (challengeNode.get("status") == null || challengeNode.get("status").asInt() != 0) {
                log.warn("Fallo al obtener challenge de UCM. Respuesta: {}", challengeRes);
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
            String loginPayload = String.format("{\"request\":{\"action\":\"login\",\"user\":\"%s\",\"token\":\"%s\",\"version\":\"1.0\"}}", apiUser, token);
            String loginRes = realizarPeticion(loginPayload);
            JsonNode loginNode = objectMapper.readTree(loginRes);
            if (loginNode.get("status") == null || loginNode.get("status").asInt() != 0) {
                log.warn("Fallo login en central UCM. Respuesta: {}", loginRes);
                int status = loginNode.path("status").asInt(-99);
                return "{\"error\":\"Fallo de autenticación en la central (status: " + status + "). Verifique que la contraseña de " + apiUser + " sea correcta.\"}";
            }
            String sessionCookie = loginNode.path("response").path("cookie").asText();

            // 4. Pedir CDR (action cdrapi oficial)
            String cdrPayload = String.format(
                "{\"request\":{\"action\":\"cdrapi\",\"cookie\":\"%s\",\"format\":\"json\"}}",
                sessionCookie
            );
            String cdrRaw = realizarPeticion(cdrPayload);
            
            // 5. Procesar y estandarizar cdr_root para el visor frontend
            return procesarCdrRoot(cdrRaw);
            
        } catch (Exception e) {
            log.error("Error al obtener CDR de UCM", e);
            return String.format("{\"error\": \"Error al conectar con la central IP: %s\"}", e.getMessage());
        }
    }

    private String procesarCdrRoot(String cdrRaw) {
        try {
            JsonNode root = objectMapper.readTree(cdrRaw);
            JsonNode cdrRootArray = root.path("cdr_root");
            
            java.util.List<ObjectNode> todasLasLlamadas = new java.util.ArrayList<>();
            
            if (cdrRootArray.isArray()) {
                for (JsonNode item : cdrRootArray) {
                    JsonNode dataNode = item.has("main_cdr") ? item.path("main_cdr") : item;
                    
                    String src = dataNode.path("src").asText("");
                    String dst = dataNode.path("dst").asText("");
                    String callerName = dataNode.path("caller_name").asText("");
                    String start = dataNode.path("start").asText("");
                    int billsec = dataNode.path("billsec").asInt(0);
                    String disposition = dataNode.path("disposition").asText("");
                    
                    // Si el main_cdr no tiene disposition, buscar en sub_cdr_1, sub_cdr_2...
                    if (disposition.isBlank()) {
                        for (int i = 1; i <= 5; i++) {
                            JsonNode sub = item.path("sub_cdr_" + i);
                            if (!sub.isMissingNode()) {
                                String subDisp = sub.path("disposition").asText("");
                                if ("ANSWERED".equalsIgnoreCase(subDisp)) {
                                    disposition = "ANSWERED";
                                    break;
                                } else if (!subDisp.isBlank()) {
                                    disposition = subDisp;
                                }
                            }
                        }
                    }
                    if (disposition.isBlank()) {
                        disposition = "NO ANSWER";
                    }

                    // Filtrar por interno (ej: 1005)
                    boolean coincideInterno = true;
                    if (internoTaller != null && !internoTaller.isBlank()) {
                        coincideInterno = src.equals(internoTaller) || dst.equals(internoTaller);
                        if (!coincideInterno) {
                            // Revisar si sonó o fue transferida al interno en sub_cdr
                            for (int i = 1; i <= 5; i++) {
                                JsonNode sub = item.path("sub_cdr_" + i);
                                if (!sub.isMissingNode()) {
                                    String subDst = sub.path("dst").asText("");
                                    String subDstExt = sub.path("dstchannel_ext").asText("");
                                    String dstAnswer = sub.path("dstanswer").asText("");
                                    if (internoTaller.equals(subDst) || internoTaller.equals(subDstExt) || internoTaller.equals(dstAnswer)) {
                                        coincideInterno = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (coincideInterno) {
                        ObjectNode callNode = objectMapper.createObjectNode();
                        callNode.put("start", start);
                        callNode.put("src", src);
                        callNode.put("dst", dst);
                        
                        boolean esSaliente = (internoTaller != null && internoTaller.equals(src));
                        String callerDisplay;
                        if (esSaliente) {
                            callerDisplay = "↗ Saliente a " + dst;
                        } else {
                            if (!callerName.isBlank() && !src.isBlank() && !callerName.equals(src)) {
                                callerDisplay = "↙ " + callerName + " (" + src + ")";
                            } else if (!callerName.isBlank()) {
                                callerDisplay = "↙ " + callerName;
                            } else if (!src.isBlank()) {
                                callerDisplay = "↙ " + src;
                            } else {
                                callerDisplay = "↙ Desconocido";
                            }
                        }
                        callNode.put("caller", callerDisplay);
                        callNode.put("billsec", billsec);
                        callNode.put("disposition", disposition);
                        
                        todasLasLlamadas.add(callNode);
                    }
                }
            }
            
            // Ordenar de más reciente a más antigua por fecha/hora
            todasLasLlamadas.sort((a, b) -> b.path("start").asText("").compareTo(a.path("start").asText("")));
            
            // Mostrar hasta 50 llamadas recientes del interno
            java.util.List<ObjectNode> seleccionadas = todasLasLlamadas;
            if (seleccionadas.size() > 50) {
                seleccionadas = seleccionadas.subList(0, 50);
            }

            ArrayNode cdrArray = objectMapper.createArrayNode();
            for (ObjectNode n : seleccionadas) {
                cdrArray.add(n);
            }

            ObjectNode responseObj = objectMapper.createObjectNode();
            responseObj.put("status", 0);
            ObjectNode respNode = objectMapper.createObjectNode();
            respNode.set("cdr", cdrArray);
            responseObj.set("response", respNode);
            
            return objectMapper.writeValueAsString(responseObj);
            
        } catch (Exception e) {
            log.error("Error procesando cdr_root de Grandstream, retornando respuesta cruda", e);
            return cdrRaw;
        }
    }

    private String realizarPeticion(String jsonPayload) throws Exception {
        URL url = new URL(baseUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setSSLSocketFactory(this.sslContext.getSocketFactory());
        // Desactivar validación estricta de Hostname SSL
        conn.setHostnameVerifier((hostname, session) -> true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");

        // Enviar TRACKID en Cookie si ya fue capturado previamente
        if (this.trackId != null && !this.trackId.isBlank()) {
            conn.setRequestProperty("Cookie", this.trackId + "; CookieName=CookieValue");
        }

        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();

        // Capturar o actualizar TRACKID de la cabecera Set-Cookie
        String setCookie = conn.getHeaderField("Set-Cookie");
        if (setCookie != null && setCookie.contains("TRACKID=")) {
            for (String part : setCookie.split(";")) {
                if (part.trim().startsWith("TRACKID=")) {
                    this.trackId = part.trim();
                    break;
                }
            }
        }

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
