package ar.gov.justiciajujuy.sanpedro.inventario.configuracion;

import java.util.Arrays;
import java.util.List;

public record TelefoniaRuntimeConfig(
		boolean enabled,
		String url,
		String user,
		String password,
		String interno,
		String webhookIps,
		boolean sslStrict,
		boolean persisted) {

	public List<String> allowedIpsList() {
		if (webhookIps == null || webhookIps.isBlank()) {
			return List.of("127.0.0.1", "0:0:0:0:0:0:0:1");
		}
		return Arrays.stream(webhookIps.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
	}

	public boolean isIpAllowed(String ip) {
		if (ip == null || ip.isBlank()) {
			return false;
		}
		List<String> allowed = allowedIpsList();
		return allowed.contains(ip.trim());
	}
}
