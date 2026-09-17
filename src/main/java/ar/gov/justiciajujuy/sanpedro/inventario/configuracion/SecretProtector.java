package ar.gov.justiciajujuy.sanpedro.inventario.configuracion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SecretProtector {

	private static final String PREFIX = "v1:";
	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;

	private final SecureRandom secureRandom = new SecureRandom();
	private final byte[] key;

	public SecretProtector(@Value("${inventario.config-secret:}") String configuredSecret) {
		this.key = deriveKey(loadSecret(configuredSecret));
	}

	public String protect(String plainText) {
		if (!StringUtils.hasText(plainText)) {
			return "";
		}
		try {
			byte[] iv = new byte[IV_BYTES];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
			byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
			byte[] payload = new byte[iv.length + encrypted.length];
			System.arraycopy(iv, 0, payload, 0, iv.length);
			System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
			return PREFIX + Base64.getEncoder().encodeToString(payload);
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("No se pudo cifrar el secreto de configuracion.", exception);
		}
	}

	public String reveal(String protectedText) {
		if (!StringUtils.hasText(protectedText)) {
			return "";
		}
		if (!protectedText.startsWith(PREFIX)) {
			throw new IllegalStateException("El secreto guardado no tiene un formato de cifrado reconocido.");
		}
		try {
			byte[] payload = Base64.getDecoder().decode(protectedText.substring(PREFIX.length()));
			if (payload.length <= IV_BYTES) {
				throw new IllegalStateException("El secreto guardado esta incompleto.");
			}
			byte[] iv = Arrays.copyOfRange(payload, 0, IV_BYTES);
			byte[] encrypted = Arrays.copyOfRange(payload, IV_BYTES, payload.length);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | IllegalArgumentException exception) {
			throw new IllegalStateException("No se pudo descifrar el secreto de configuracion.", exception);
		}
	}

	private String loadSecret(String configuredSecret) {
		if (StringUtils.hasText(configuredSecret)) {
			return configuredSecret;
		}
		// En desarrollo se genera una clave local ignorada por git; en produccion debe
		// fijarse INVENTARIO_CONFIG_SECRET para poder descifrar despues de reinicios o despliegues.
		Path secretPath = Path.of(".local-secrets", "inventario-config.key");
		try {
			Files.createDirectories(secretPath.getParent());
			if (Files.exists(secretPath)) {
				return Files.readString(secretPath, StandardCharsets.UTF_8).trim();
			}
			byte[] secret = new byte[32];
			secureRandom.nextBytes(secret);
			String generated = Base64.getEncoder().encodeToString(secret);
			Files.writeString(secretPath, generated, StandardCharsets.UTF_8);
			return generated;
		} catch (IOException exception) {
			throw new IllegalStateException(
					"No se pudo preparar la clave local de cifrado. Configure INVENTARIO_CONFIG_SECRET.", exception);
		}
	}

	private byte[] deriveKey(String secret) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("No se pudo derivar la clave de cifrado.", exception);
		}
	}
}
