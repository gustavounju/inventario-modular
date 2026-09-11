package ar.gov.justiciajujuy.sanpedro.inventario.movil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DictadoTareaAiService {
	private static final int MAX_TEXTO = 2000;
	private static final Pattern LIMPIEZA_USUARIO = Pattern.compile("[^a-z0-9.]+");

	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final boolean enabled;
	private final String apiKey;
	private final String endpoint;
	private final String model;

	public DictadoTareaAiService(@Value("${inventario.ia.openai.enabled:false}") boolean enabled,
			@Value("${inventario.ia.openai.api-key:${OPENAI_API_KEY:}}") String apiKey,
			@Value("${inventario.ia.openai.endpoint:https://api.openai.com/v1/responses}") String endpoint,
			@Value("${inventario.ia.openai.model:gpt-4.1-mini}") String model) {
		this.objectMapper = new ObjectMapper();
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
		this.enabled = enabled;
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.endpoint = endpoint;
		this.model = model;
	}

	public InterpretacionDictado interpretar(String texto, String usuarioFallback, String nombreFallback, String fueroFallback) {
		String limpio = limpiarTexto(texto);
		if (limpio.isBlank()) {
			throw new IllegalArgumentException("El dictado no puede estar vacio.");
		}
		if (enabled && !apiKey.isBlank()) {
			try {
				return interpretarConOpenAi(limpio, usuarioFallback, nombreFallback, fueroFallback);
			} catch (Exception ignored) {
				// Si la red o la IA fallan, el tecnico igual recibe una tarea precargada para revisar.
			}
		}
		return interpretarLocal(limpio, usuarioFallback, nombreFallback, fueroFallback,
				enabled ? "IA no disponible en este momento; se uso interpretacion local." : "IA deshabilitada; se uso interpretacion local.");
	}

	private InterpretacionDictado interpretarConOpenAi(String texto, String usuarioFallback, String nombreFallback,
			String fueroFallback) throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", model);
		body.put("input", List.of(
				Map.of("role", "system", "content", """
						Sos un asistente interno para mesa tecnica. Extrae de un dictado breve:
						solicitanteNombre, titulo y descripcion. Si no se menciona el solicitante,
						usa el operador como solicitante. Responde solo JSON valido con claves:
						solicitanteNombre, titulo, descripcion, prioridad, confianza.
						Prioridad debe ser BAJA, MEDIA, ALTA o URGENTE.
						"""),
				Map.of("role", "user", "content", "Operador: " + nombreFallback + "\nOficina: " + fueroFallback
						+ "\nDictado: " + texto)));
		body.put("temperature", 0.1);
		HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
				.timeout(Duration.ofSeconds(20))
				.header("Authorization", "Bearer " + apiKey)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
				.build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IllegalStateException("La IA respondio con estado " + response.statusCode());
		}
		String jsonText = extraerTextoRespuesta(objectMapper.readTree(response.body()));
		JsonNode parsed = objectMapper.readTree(jsonText);
		String solicitante = texto(parsed, "solicitanteNombre", nombreFallback);
		String titulo = limitar(texto(parsed, "titulo", texto), 180);
		String descripcion = limitar(texto(parsed, "descripcion", texto), 1000);
		String prioridad = prioridad(texto(parsed, "prioridad", "MEDIA"));
		double confianza = parsed.path("confianza").asDouble(0.75);
		return new InterpretacionDictado(true, "OPENAI", solicitante, usuarioDesdeNombre(solicitante, usuarioFallback),
				fueroFallback, titulo, descripcion, prioridad, confianza, "IA real aplicada. Revise antes de guardar.");
	}

	private String extraerTextoRespuesta(JsonNode root) {
		if (root.hasNonNull("output_text")) return root.get("output_text").asText();
		JsonNode output = root.path("output");
		if (output.isArray()) {
			for (JsonNode item : output) {
				JsonNode content = item.path("content");
				if (content.isArray()) {
					for (JsonNode part : content) {
						if (part.hasNonNull("text")) return part.get("text").asText();
					}
				}
			}
		}
		throw new IllegalStateException("La IA no devolvio texto interpretable.");
	}

	private InterpretacionDictado interpretarLocal(String texto, String usuarioFallback, String nombreFallback,
			String fueroFallback, String mensaje) {
		String solicitante = nombreFallback;
		var solicitanteMatcher = Pattern.compile("\\b(?:solicita|solicitante|pidio|pidió|pide|para|de)\\s+([^,.;]+?)(?:\\s+(?:por|porque|que|indica|dice|tiene|no|se)\\b|[,.;]|$)",
				Pattern.CASE_INSENSITIVE).matcher(texto);
		if (solicitanteMatcher.find()) solicitante = solicitanteMatcher.group(1).trim();
		String problema = texto;
		var problemaMatcher = Pattern.compile("\\b(?:problema|inconveniente|falla|fallo|error|porque|que|indica|dice)\\b\\s*(.+)$",
				Pattern.CASE_INSENSITIVE).matcher(texto);
		if (problemaMatcher.find()) problema = problemaMatcher.group(1).trim();
		return new InterpretacionDictado(false, "LOCAL", solicitante, usuarioDesdeNombre(solicitante, usuarioFallback),
				fueroFallback, limitar(problema.isBlank() ? "Tarea dictada" : problema, 180), limitar(texto, 1000),
				"MEDIA", 0.35, mensaje);
	}

	private String limpiarTexto(String texto) {
		if (texto == null) return "";
		String limpio = texto.trim().replaceAll("\\s+", " ");
		return limpio.length() > MAX_TEXTO ? limpio.substring(0, MAX_TEXTO) : limpio;
	}

	private String texto(JsonNode node, String field, String fallback) {
		String value = node.path(field).asText(fallback == null ? "" : fallback).trim();
		return value.isBlank() ? fallback : value;
	}

	private String prioridad(String value) {
		String normalizada = value == null ? "MEDIA" : value.trim().toUpperCase(Locale.ROOT);
		return switch (normalizada) {
			case "BAJA", "MEDIA", "ALTA", "URGENTE" -> normalizada;
			default -> "MEDIA";
		};
	}

	private String usuarioDesdeNombre(String nombre, String fallback) {
		String usuario = LIMPIEZA_USUARIO.matcher(nombre.toLowerCase(Locale.ROOT)
				.replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
				.replace("ñ", "n")).replaceAll(".").replaceAll("^\\.+|\\.+$", "");
		return usuario.isBlank() ? fallback : usuario;
	}

	private String limitar(String value, int max) {
		if (value == null) return "";
		String limpio = value.trim();
		return limpio.length() <= max ? limpio : limpio.substring(0, max);
	}

	public record InterpretacionDictado(boolean iaDisponible, String origen, String solicitanteNombre,
			String solicitanteUsername, String solicitanteFuero, String titulo, String descripcion, String prioridad,
			double confianza, String mensaje) {
	}
}
