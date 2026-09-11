package ar.gov.justiciajujuy.sanpedro.inventario.movil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

@Service
public class ApkDistributionService {

	private final FileSystemResource apk;
	private final String versionPublicada;

	public ApkDistributionService(
			@Value("${inventario.movil.apk-path:output/android/tecnico-taller-san-pedro-lan-release.apk}") String apkPath,
			@Value("${inventario.movil.apk-version:desconocida}") String versionPublicada) {
		this.apk = new FileSystemResource(apkPath);
		this.versionPublicada = versionPublicada;
	}

	public FileSystemResource resource() {
		return apk;
	}

	public boolean isAvailable() {
		return apk.isReadable();
	}

	public ApkInfo info() {
		if (!isAvailable()) {
			return new ApkInfo(false, apk.getPath(), null, versionPublicada, 0, null, null);
		}
		try {
			var path = apk.getFile().toPath();
			// La huella se expone para diagnostico de distribucion, no como dato de uso diario.
			return new ApkInfo(true, path.toString(), apk.getFilename(), versionPublicada, Files.size(path),
					Files.getLastModifiedTime(path).toInstant(), sha256(path));
		} catch (IOException ex) {
			return new ApkInfo(false, apk.getPath(), apk.getFilename(), versionPublicada, 0, null, null);
		}
	}

	private String sha256(java.nio.file.Path path) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (InputStream input = Files.newInputStream(path);
					DigestInputStream digestInput = new DigestInputStream(input, digest)) {
				digestInput.transferTo(OutputStream.nullOutputStream());
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 no disponible en la JVM.", ex);
		}
	}

	public record ApkInfo(boolean disponible, String ruta, String nombre, String version, long bytes, Instant actualizadoEn,
			String sha256) {
	}
}
