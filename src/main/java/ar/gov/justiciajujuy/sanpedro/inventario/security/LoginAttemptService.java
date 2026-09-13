package ar.gov.justiciajujuy.sanpedro.inventario.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginAttemptService {

	private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();
	private final int maxFailures;
	private final Duration blockDuration;
	private final Clock clock;

	@Autowired
	public LoginAttemptService(
			@Value("${inventario.security.login.max-failures:5}") int maxFailures,
			@Value("${inventario.security.login.block-seconds:300}") long blockSeconds) {
		this(maxFailures, Duration.ofSeconds(blockSeconds), Clock.systemUTC());
	}

	LoginAttemptService(int maxFailures, Duration blockDuration, Clock clock) {
		this.maxFailures = Math.max(1, maxFailures);
		this.blockDuration = blockDuration.isNegative() || blockDuration.isZero()
				? Duration.ofMinutes(5)
				: blockDuration;
		this.clock = clock;
	}

	public boolean isBlocked(String username, String remoteAddress) {
		AttemptState state = attempts.get(key(username, remoteAddress));
		if (state == null) {
			return false;
		}
		if (state.blockedUntil() == null) {
			return false;
		}
		if (Instant.now(clock).isBefore(state.blockedUntil())) {
			return true;
		}
		attempts.remove(key(username, remoteAddress), state);
		return false;
	}

	public void loginFailed(String username, String remoteAddress) {
		String key = key(username, remoteAddress);
		attempts.compute(key, (ignored, current) -> {
			int failures = current == null ? 1 : current.failures() + 1;
			Instant blockedUntil = failures >= maxFailures ? Instant.now(clock).plus(blockDuration) : null;
			return new AttemptState(failures, blockedUntil);
		});
	}

	public void loginSucceeded(String username, String remoteAddress) {
		attempts.remove(key(username, remoteAddress));
	}

	private String key(String username, String remoteAddress) {
		String normalizedUser = StringUtils.hasText(username)
				? username.trim().toLowerCase(Locale.ROOT)
				: "anonymous";
		String normalizedAddress = StringUtils.hasText(remoteAddress) ? remoteAddress.trim() : "unknown";
		return normalizedUser + "@" + normalizedAddress;
	}

	private record AttemptState(int failures, Instant blockedUntil) {
	}
}
